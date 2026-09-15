package com.mateirobescu.thesis.exception;

public abstract class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
