
package com.weatherconnect.dto;

public record WeatherSummary(
        String city,
        String country,
        double latitude,
        double longitude,
        CurrentWeather current
) {
}
