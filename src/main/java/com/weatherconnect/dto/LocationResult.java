
package com.weatherconnect.dto;

public record LocationResult(
        String name,
        double latitude,
        double longitude,
        String country
) {
}
