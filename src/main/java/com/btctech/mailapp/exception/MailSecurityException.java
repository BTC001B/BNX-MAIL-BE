package com.btctech.mailapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a security threat (e.g. virus) is detected.
 * Maps to 422 Unprocessable Entity as per REST best practices for semantic errors.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class MailSecurityException extends RuntimeException {
    public MailSecurityException(String message) {
        super(message);
    }
}
