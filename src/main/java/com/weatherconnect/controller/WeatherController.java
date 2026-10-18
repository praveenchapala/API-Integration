
package com.weatherconnect.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.weatherconnect.client.WeatherApiClient;
import com.weatherconnect.dto.WeatherSummary;

@RestController
public class WeatherController {

    private final WeatherApiClient weatherApiClient;

    public WeatherController(WeatherApiClient weatherApiClient) {
        this.weatherApiClient = weatherApiClient;
    }

    // Check whether the backend is running
    @GetMapping("/api/health")
    public String healthCheck() {
        return "WeatherConnect backend is running!";
    }

    // Retrieve current weather using a city name
    @GetMapping("/api/weather")
    public WeatherSummary getWeather(
            @RequestParam(required = false) String city) {

        if (city == null || city.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "City parameter is required");
        }

        return weatherApiClient.getWeatherByCity(city.trim());
    }
}
