
package com.weatherconnect.exception;

import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle cities that cannot be found.
    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCityNotFound(
            CityNotFoundException exception) {

        return buildError(
                HttpStatus.NOT_FOUND,
                "CITY_NOT_FOUND",
                exception.getMessage());
    }

    // Handle invalid requests, such as missing city names.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            ResponseStatusException exception) {

        HttpStatus status = HttpStatus.resolve(
                exception.getStatusCode().value());

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String code = status == HttpStatus.BAD_REQUEST
                ? "INVALID_REQUEST"
                : "REQUEST_FAILED";

        return buildError(
                status,
                code,
                exception.getReason() == null
                        ? "The request could not be processed."
                        : exception.getReason());
    }

    // Handle connection failures and timeouts.
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleConnectionFailure(
            ResourceAccessException exception) {

        if (hasTimeoutCause(exception)) {
            return buildError(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "WEATHER_SERVICE_TIMEOUT",
                    "The weather service took too long to respond. Please try again.");
        }

        return buildError(
                HttpStatus.BAD_GATEWAY,
                "WEATHER_SERVICE_UNAVAILABLE",
                "The external weather service is currently unavailable.");
    }

    // Handle other external HTTP client failures.
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleExternalApiError(
            RestClientException exception) {

        return buildError(
                HttpStatus.BAD_GATEWAY,
                "WEATHER_SERVICE_ERROR",
                "The external weather service request failed.");
    }

    // Handle missing or unusable forecast data.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleUnavailableWeather(
            IllegalStateException exception) {

        return buildError(
                HttpStatus.BAD_GATEWAY,
                "WEATHER_DATA_UNAVAILABLE",
                "Weather data is currently unavailable.");
    }

    // Fallback for unexpected server-side errors.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedError(
            Exception exception) {

        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<Map<String, String>> buildError(
            HttpStatus status,
            String code,
            String message) {

        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "error", code,
                        "message", message));
    }

    private boolean hasTimeoutCause(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}
