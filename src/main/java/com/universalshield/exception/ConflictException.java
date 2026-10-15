package com.universalshield.exception;

import org.springframework.http.HttpStatus;

/** 409, e.g. registering an email that already exists. */
public class ConflictException extends ApiException {

    public ConflictException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
