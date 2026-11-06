package com.mateirobescu.thesis.exception;

import lombok.Getter;

@Getter
public class ConflictException extends ApiException {

    private String field;

    public ConflictException(String field, String message) {
        this.field = field;
        super(message);
    }

    public ConflictException(String message) {
        super(message);
    }

}
