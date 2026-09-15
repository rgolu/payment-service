package com.payments.domain.exception;

/** Same-key replay with a different payload — ppisvc {@code DuplicateEntityException}. */
public class DuplicateEntityException extends DomainException {

    public DuplicateEntityException(String message) {
        super(409, message);
    }
}
