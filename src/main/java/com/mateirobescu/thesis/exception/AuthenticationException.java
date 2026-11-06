package com.mateirobescu.thesis.exception;

public class AuthenticationException extends ApiException {
    public AuthenticationException(String message) {
        super(message);
    }
}
