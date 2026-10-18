
package com.weatherconnect.dto;

public record CurrentWeather(
        String time,
        double temperature_2m,
        int relative_humidity_2m,
        int weather_code,
        double wind_speed_10m
) {
}
