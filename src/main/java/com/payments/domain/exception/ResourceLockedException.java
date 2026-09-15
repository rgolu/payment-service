package com.payments.domain.exception;

/** Lock not acquired within timeout — ppisvc {@code TemporaryResourceLockException}. */
public class ResourceLockedException extends DomainException {

    public ResourceLockedException(String message) {
        super(423, message);
    }
}
