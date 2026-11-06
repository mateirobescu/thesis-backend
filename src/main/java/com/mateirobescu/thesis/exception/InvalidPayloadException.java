package com.mateirobescu.thesis.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Thrown when a payload nested in a request body does not have the structure its type requires.
 */
@Getter
public class InvalidPayloadException extends ApiException {

    private final Map<String, List<String>> errors;

    public InvalidPayloadException(String message, Map<String, List<String>> errors) {
        super(message);
        this.errors = errors;
    }
}
