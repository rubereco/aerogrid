package com.aerogrid.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;

/**
 * Global exception handler for all REST controllers.
 * Provides consistent error responses across the application.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles missing required request parameters.
     *
     * @param ex the exception
     * @return 400 Bad Request response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingParams(MissingServletRequestParameterException ex) {
        log.error("Missing required parameter: {}", ex.getParameterName());
        return ResponseEntity.badRequest()
                .body("Missing required parameter: " + ex.getParameterName());
    }

    /**
     * Handles missing required request headers.
     *
     * @param ex the exception
     * @return 400 Bad Request response
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<String> handleMissingHeader(MissingRequestHeaderException ex) {
        log.error("Missing required header: {}", ex.getHeaderName());
        return ResponseEntity.badRequest()
                .body("Missing required header: " + ex.getHeaderName());
    }

    /**
     * Handles invalid JSON or malformed request body.
     *
     * @param ex the exception
     * @return 400 Bad Request response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleInvalidJson(HttpMessageNotReadableException ex) {
        log.error("Invalid request body: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body("Invalid request body: malformed JSON or invalid format");
    }

    /**
     * Handles invalid parameter type conversions.
     *
     * @param ex the exception
     * @return 400 Bad Request response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("Invalid parameter type: {} for parameter {}", ex.getValue(), ex.getName());
        return ResponseEntity.badRequest()
                .body("Invalid parameter value: " + ex.getValue() + " for parameter: " + ex.getName());
    }

    /**
     * Handles date/time parsing errors.
     *
     * @param ex the exception
     * @return 400 Bad Request response
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<String> handleDateTimeParseException(DateTimeParseException ex) {
        log.error("Invalid date/time format: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body("Invalid date/time format. Please use ISO 8601 format (e.g., 2026-01-29T10:30:00)");
    }

    /**
     * Handles illegal argument exceptions.
     *
     * @param ex the exception
     * @return 400 Bad Request response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body("Invalid argument: " + ex.getMessage());
    }

    /**
     * Handles all other runtime exceptions.
     *
     * @param ex the exception
     * @return 500 Internal Server Error response
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error: " + ex.getMessage());
    }

    /**
     * Handles all other exceptions not caught by specific handlers.
     *
     * @param ex the exception
     * @return 500 Internal Server Error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred: " + ex.getMessage());
    }
}

