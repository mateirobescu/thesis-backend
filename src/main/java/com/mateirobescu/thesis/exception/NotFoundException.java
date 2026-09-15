package com.mateirobescu.thesis.exception;

public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String resource, Object id) {
        super(String.format("%s not found: %s", resource, id));
    }
}
