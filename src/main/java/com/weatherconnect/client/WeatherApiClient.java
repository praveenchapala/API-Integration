
package com.weatherconnect.client;

import java.net.http.HttpClient;
import java.time.Duration;

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

    private final RestClient restClient;

    public WeatherApiClient() {
        // Maximum time allowed to establish a connection.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        // Maximum time allowed for an individual HTTP request.
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public WeatherSummary getWeatherByCity(String city) {

        // 1. Build the geocoding API URL.
        String geocodingUrl = UriComponentsBuilder
                .fromUriString(
                        "https://geocoding-api.open-meteo.com/v1/search")
                .queryParam("name", city)
                .queryParam("count", 1)
                .queryParam("language", "en")
                .queryParam("format", "json")
                .encode()
                .toUriString();

        // 2. Call the geocoding API and deserialize its JSON.
        GeocodingResponse geocodingResponse = restClient.get()
                .uri(geocodingUrl)
                .retrieve()
                .body(GeocodingResponse.class);

        // 3. Handle a city that was not found.
        if (geocodingResponse == null
                || geocodingResponse.results() == null
                || geocodingResponse.results().isEmpty()) {
            throw new CityNotFoundException(city);
        }

        LocationResult location = geocodingResponse.results().get(0);

        // 4. Build the forecast API URL.
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

        // 5. Call the forecast API and deserialize its JSON.
        ForecastResponse forecastResponse = restClient.get()
                .uri(forecastUrl)
                .retrieve()
                .body(ForecastResponse.class);

        // 6. Validate the forecast response.
        if (forecastResponse == null
                || forecastResponse.current() == null) {
            throw new IllegalStateException(
                    "Weather data is unavailable for: " + city);
        }

        CurrentWeather current = forecastResponse.current();

        // 7. Return the structured application response.
        return new WeatherSummary(
                location.name(),
                location.country(),
                forecastResponse.latitude(),
                forecastResponse.longitude(),
                current);
    }
}
