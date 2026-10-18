
package com.weatherconnect.dto;

public record ForecastResponse(
        double latitude,
        double longitude,
        CurrentWeather current
) {
}
