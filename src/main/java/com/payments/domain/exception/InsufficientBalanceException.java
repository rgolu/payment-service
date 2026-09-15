package com.payments.domain.exception;

public class InsufficientBalanceException extends DomainException {

    public InsufficientBalanceException(String message) {
        super(409, message);
    }
}
