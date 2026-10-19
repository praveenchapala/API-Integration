
package com.weatherconnect.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.weatherconnect.dto.CurrentWeather;
import com.weatherconnect.dto.ForecastResponse;
import com.weatherconnect.dto.GeocodingResponse;
import com.weatherconnect.dto.LocationResult;
import com.weatherconnect.dto.WeatherSummary;
import com.weatherconnect.exception.CityNotFoundException;

@Component
public class WeatherApiClient {

    private static final Logger log =
            LoggerFactory.getLogger(WeatherApiClient.class);

    // Retry only transient connection failures and HTTP 5xx responses.
    // Do not retry HTTP 429 rate-limit responses.
    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 500;

    // Cache entries expire after 10 minutes.
    private static final long CACHE_TTL_MS =
            Duration.ofMinutes(10).toMillis();

    private static final String GEOCODING_URL =
            "https://geocoding-api.open-meteo.com/v1/search";

    private static final String FORECAST_URL =
            "https://api.open-meteo.com/v1/forecast";

    private final RestClient restClient;

    private final Map<String, CacheEntry<LocationResult>> locationCache =
            new ConcurrentHashMap<>();

    private final Map<String, CacheEntry<WeatherSummary>> weatherCache =
            new ConcurrentHashMap<>();

    public WeatherApiClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public WeatherSummary getWeatherByCity(String city) {
        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException(
                    "City name must not be empty.");
        }

        String normalizedCity = city.trim();
        String cityKey = normalizedCity.toLowerCase(Locale.ROOT);

        // Return cached weather when the entry is still valid.
        WeatherSummary cachedWeather = getCached(
                weatherCache, cityKey);

        if (cachedWeather != null) {
            log.info("Weather cache hit for city: {}", normalizedCity);
            return cachedWeather;
        }

        // Reuse previously resolved city coordinates.
        LocationResult location = getLocation(normalizedCity, cityKey);

        WeatherSummary weather = fetchWeather(location, normalizedCity);

        weatherCache.put(
                cityKey,
                new CacheEntry<>(weather, System.currentTimeMillis()));

        return weather;
    }

    private LocationResult getLocation(String city, String cityKey) {
        LocationResult cachedLocation = getCached(
                locationCache, cityKey);

        if (cachedLocation != null) {
            log.info("Geocoding cache hit for city: {}", city);
            return cachedLocation;
        }

        String geocodingUrl = UriComponentsBuilder
                .fromUriString(GEOCODING_URL)
                .queryParam("name", city)
                .queryParam("count", 1)
                .queryParam("language", "en")
                .queryParam("format", "json")
                .build()
                .encode()
                .toUriString();

        log.info("Starting geocoding request for city: {}", city);

        GeocodingResponse response = executeWithRetry(
                "geocoding",
                city,
                () -> restClient.get()
                        .uri(geocodingUrl)
                        .retrieve()
                        .body(GeocodingResponse.class));

        if (response == null
                || response.results() == null
                || response.results().isEmpty()) {
            throw new CityNotFoundException(city);
        }

        LocationResult location = response.results().get(0);

        locationCache.put(
                cityKey,
                new CacheEntry<>(location, System.currentTimeMillis()));

        return location;
    }

    private WeatherSummary fetchWeather(
            LocationResult location,
            String city) {

        String forecastUrl = UriComponentsBuilder
                .fromUriString(FORECAST_URL)
                .queryParam("latitude", location.latitude())
                .queryParam("longitude", location.longitude())
                .queryParam(
                        "current",
                        "temperature_2m,relative_humidity_2m,"
                                + "weather_code,wind_speed_10m")
                .build()
                .encode()
                .toUriString();

        log.info("Starting forecast request for city: {}", city);

        ForecastResponse response = executeWithRetry(
                "forecast",
                city,
                () -> restClient.get()
                        .uri(forecastUrl)
                        .retrieve()
                        .body(ForecastResponse.class));

        if (response == null || response.current() == null) {
            throw new IllegalStateException(
                    "Weather data is unavailable for: " + city);
        }

        CurrentWeather current = response.current();

        return new WeatherSummary(
                location.name(),
                location.country(),
                response.latitude(),
                response.longitude(),
                current);
    }

    private <T> T executeWithRetry(
            String operation,
            String city,
            ApiCall<T> apiCall) {

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                T response = apiCall.execute();

                log.info(
                        "{} response received for city: {}",
                        operation,
                        city);

                return response;

            } catch (ResourceAccessException ex) {
                log.warn(
                        "{} connection failed for city {} "
                                + "(attempt {}/{}): {}",
                        operation,
                        city,
                        attempt,
                        MAX_ATTEMPTS,
                        ex.getMessage());

                if (attempt == MAX_ATTEMPTS
                        || !isTransientConnectionError(ex)) {
                    throw ex;
                }

                pauseBeforeRetry(operation, city);

            } catch (RestClientResponseException ex) {
                HttpStatusCode status = ex.getStatusCode();

                // Limit the body logged to avoid excessively large log entries.
                String responseBody = ex.getResponseBodyAsString();
                if (responseBody.length() > 1000) {
                    responseBody = responseBody.substring(0, 1000);
                }

                log.warn(
                        "{} returned HTTP {} for city {}. "
                                + "Provider response: {}",
                        operation,
                        status.value(),
                        city,
                        responseBody);

                // Rate limiting is not a transient 5xx error.
                // Propagate it immediately instead of retrying.
                if (status.value() == 429) {
                    throw ex;
                }

                if (attempt == MAX_ATTEMPTS
                        || !status.is5xxServerError()) {
                    throw ex;
                }

                pauseBeforeRetry(operation, city);
            }
        }

        throw new IllegalStateException(
                "Unexpected retry termination for " + operation);
    }

    private boolean isTransientConnectionError(
            ResourceAccessException exception) {

        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof SocketTimeoutException
                    || cause instanceof ConnectException
                    || cause instanceof IOException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    private void pauseBeforeRetry(String operation, String city) {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Retry interrupted for " + operation
                            + " request for city " + city,
                    ex);
        }
    }

    private <T> T getCached(
            Map<String, CacheEntry<T>> cache,
            String key) {

        CacheEntry<T> entry = cache.get(key);

        if (entry == null) {
            return null;
        }

        long age = System.currentTimeMillis() - entry.createdAt();

        if (age >= 0 && age < CACHE_TTL_MS) {
            return entry.value();
        }

        cache.remove(key, entry);
        return null;
    }

    private record CacheEntry<T>(T value, long createdAt) {
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute();
    }
}
