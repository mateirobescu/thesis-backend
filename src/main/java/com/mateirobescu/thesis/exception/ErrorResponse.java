package com.mateirobescu.thesis.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        int status,
        String code,
        String message,
        Map<String, List<String>> errors,
        String path,
        Instant timestamp
) {
    public static ErrorResponse of(HttpStatus status, String code, String message, String path) {
        return new ErrorResponse(status.value(), code, message, null, path, Instant.now());
    }

    public static ErrorResponse validation(Map<String, List<String>> fieldErrors, String path) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED", "Please fix the highlighted fields.",
                fieldErrors, path, Instant.now());
    }

    public static ErrorResponse conflict(String message, Map<String, List<String>> fieldErrors, String path) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), "CONFLICT", message, fieldErrors, path, Instant.now());
    }
}
