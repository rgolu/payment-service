package com.payments.domain.exception;

public class CouponException extends DomainException {

    public CouponException(String message) {
        super(400, message);
    }
}
