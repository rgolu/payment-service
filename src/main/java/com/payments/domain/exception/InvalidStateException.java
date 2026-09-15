package com.payments.domain.exception;

public class InvalidStateException extends DomainException {

    public InvalidStateException(String message) {
        super(409, message);
    }
}
