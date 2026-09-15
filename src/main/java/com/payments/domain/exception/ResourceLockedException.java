package com.payments.domain.exception;

/** Lock not acquired within the wait timeout. */
public class ResourceLockedException extends DomainException {

    public ResourceLockedException(String message) {
        super(423, message);
    }
}
