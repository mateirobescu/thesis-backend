package com.mateirobescu.thesis.exception;

public class SessionExpiredException extends AuthenticationException {

    public SessionExpiredException() {
        super("Session has expired. Please login again.");
    }
    
}
