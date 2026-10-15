package com.universalshield.exception;

import org.springframework.http.HttpStatus;

/**
 * 404. Also used when a resource exists but belongs to another user, so the API never reveals
 * whether someone else's scan id is valid.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
