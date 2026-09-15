package com.payments.domain.exception;

/** Same idempotency key replayed with a different payload. */
public class DuplicateEntityException extends DomainException {

    public DuplicateEntityException(String message) {
        super(409, message);
    }
}
