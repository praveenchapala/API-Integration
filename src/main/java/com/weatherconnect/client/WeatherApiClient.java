
package com.weatherconnect.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.time.Duration;

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

    private static final int MAX_ATTEMPTS = 2;

    private final RestClient restClient;

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

        String geocodingUrl = UriComponentsBuilder
                .fromUriString(
                        "https://geocoding-api.open-meteo.com/v1/search")
                .queryParam("name", city)
                .queryParam("count", 1)
                .queryParam("language", "en")
                .queryParam("format", "json")
                .encode()
                .toUriString();

        log.info("Starting geocoding request for city: {}", city);

        GeocodingResponse geocodingResponse =
                executeWithRetry(
                        "geocoding",
                        city,
                        () -> restClient.get()
                                .uri(geocodingUrl)
                                .retrieve()
                                .body(GeocodingResponse.class));

        if (geocodingResponse == null
                || geocodingResponse.results() == null
                || geocodingResponse.results().isEmpty()) {
            throw new CityNotFoundException(city);
        }

        LocationResult location = geocodingResponse.results().get(0);

        String forecastUrl = UriComponentsBuilder
                .fromUriString(
                        "https://api.open-meteo.com/v1/forecast")
                .queryParam("latitude", location.latitude())
                .queryParam("longitude", location.longitude())
                .queryParam(
                        "current",
                        "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m")
                .encode()
                .toUriString();

        log.info("Starting forecast request for city: {}", city);

        ForecastResponse forecastResponse =
                executeWithRetry(
                        "forecast",
                        city,
                        () -> restClient.get()
                                .uri(forecastUrl)
                                .retrieve()
                                .body(ForecastResponse.class));

        if (forecastResponse == null
                || forecastResponse.current() == null) {
            throw new IllegalStateException(
                    "Weather data is unavailable for: " + city);
        }

        CurrentWeather current = forecastResponse.current();

        return new WeatherSummary(
                location.name(),
                location.country(),
                forecastResponse.latitude(),
                forecastResponse.longitude(),
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
                        "{} connection failed for city {} (attempt {}/{}): {}",
                        operation,
                        city,
                        attempt,
                        MAX_ATTEMPTS,
                        ex.getMessage());

                if (attempt == MAX_ATTEMPTS || !isTransientConnectionError(ex)) {
                    throw ex;
                }

                pauseBeforeRetry(operation, city);

            } catch (RestClientResponseException ex) {
                HttpStatusCode status = ex.getStatusCode();

                log.warn(
                        "{} returned HTTP {} for city {}",
                        operation,
                        status.value(),
                        city);

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
            Thread.sleep(500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Retry interrupted for " + operation
                            + " request for city " + city,
                    ex);
        }
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute();
    }
}
