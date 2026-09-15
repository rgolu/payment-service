package com.payments.domain.exception;

public class PaymentException extends DomainException {

    public PaymentException(String message) {
        super(400, message);
    }
}
