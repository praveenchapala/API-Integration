
package com.weatherconnect.client;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
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

    private final RestClient restClient;

    public WeatherApiClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
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

        GeocodingResponse geocodingResponse;

        try {
            geocodingResponse = restClient.get()
                    .uri(geocodingUrl)
                    .retrieve()
                    .body(GeocodingResponse.class);

            log.info("Geocoding response received for city: {}", city);

        } catch (Exception ex) {
            log.error("Geocoding request failed for city: {}", city, ex);
            throw ex;
        }

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

        ForecastResponse forecastResponse;

        try {
            forecastResponse = restClient.get()
                    .uri(forecastUrl)
                    .retrieve()
                    .body(ForecastResponse.class);

            log.info("Forecast response received for city: {}", city);

        } catch (Exception ex) {
            log.error("Forecast request failed for city: {}", city, ex);
            throw ex;
        }

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
}
