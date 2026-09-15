package com.payments.domain.exception;

public class ProviderUnavailableException extends DomainException {

    public ProviderUnavailableException(String message) {
        super(503, message);
    }
}
