package com.example.canvasia.exception;

import lombok.Getter;

@Getter
public class DomainValidationException extends RuntimeException {

    private final String errorCode;

    public DomainValidationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
