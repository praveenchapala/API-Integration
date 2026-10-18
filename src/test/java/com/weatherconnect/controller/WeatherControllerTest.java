
package com.weatherconnect.controller;

import java.net.http.HttpTimeoutException;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.weatherconnect.client.WeatherApiClient;
import com.weatherconnect.dto.CurrentWeather;
import com.weatherconnect.dto.WeatherSummary;
import com.weatherconnect.exception.CityNotFoundException;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherApiClient weatherApiClient;

    // Test 1: Health endpoint returns HTTP 200
    @Test
    void healthEndpointShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    // Test 2: Missing city returns HTTP 400
    @Test
    void missingCityShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/weather"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(weatherApiClient);
    }

    // Test 3: Valid city returns weather data
    @Test
    void validCityShouldReturnWeatherData() throws Exception {
        CurrentWeather current = new CurrentWeather(
                "2026-10-18T14:00",
                25.8,
                68,
                3,
                9.0
        );

        WeatherSummary summary = new WeatherSummary(
                "Bengaluru",
                "India",
                12.97,
                77.59,
                current
        );

        when(weatherApiClient.getWeatherByCity("Bengaluru"))
                .thenReturn(summary);

        mockMvc.perform(get("/api/weather")
                        .param("city", "Bengaluru"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("Bengaluru"))
                .andExpect(jsonPath("$.country").value("India"))
                .andExpect(jsonPath("$.current.temperature_2m").value(25.8))
                .andExpect(jsonPath("$.current.relative_humidity_2m").value(68))
                .andExpect(jsonPath("$.current.weather_code").value(3))
                .andExpect(jsonPath("$.current.wind_speed_10m").value(9.0));

        verify(weatherApiClient).getWeatherByCity("Bengaluru");
    }

    // Test 4: Unknown city returns HTTP 404
    @Test
    void unknownCityShouldReturnNotFound() throws Exception {
        when(weatherApiClient.getWeatherByCity("Atlantis"))
                .thenThrow(new CityNotFoundException("Atlantis"));

        mockMvc.perform(get("/api/weather")
                        .param("city", "Atlantis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("CITY_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("City not found: Atlantis"));
    }

    // Test 5: Blank city returns HTTP 400
    @Test
    void blankCityShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/weather")
                        .param("city", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("City parameter is required"));

        verifyNoInteractions(weatherApiClient);
    }

    // Test 6: External weather service failure returns HTTP 502
    @Test
    void externalWeatherServiceFailureShouldReturnBadGateway()
            throws Exception {
        when(weatherApiClient.getWeatherByCity("Bengaluru"))
                .thenThrow(new RestClientException(
                        "External weather service failed"));

        mockMvc.perform(get("/api/weather")
                        .param("city", "Bengaluru"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error")
                        .value("WEATHER_SERVICE_ERROR"));
    }

    // Test 7: External weather service timeout returns HTTP 504
    @Test
    void externalWeatherServiceTimeoutShouldReturnGatewayTimeout()
            throws Exception {
        ResourceAccessException timeout =
                new ResourceAccessException(
                        "Weather service timed out",
                        new HttpTimeoutException("Request timed out"));

        when(weatherApiClient.getWeatherByCity("Bengaluru"))
                .thenThrow(timeout);

        mockMvc.perform(get("/api/weather")
                        .param("city", "Bengaluru"))
                .andExpect(status().isGatewayTimeout());
    }
}
