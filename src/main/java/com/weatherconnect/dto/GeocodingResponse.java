
package com.weatherconnect.dto;

import java.util.List;

public record GeocodingResponse(
        List<LocationResult> results
) {
}
