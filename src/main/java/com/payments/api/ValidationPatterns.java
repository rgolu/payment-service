package com.payments.api;

/**
 * Request regexes aligned with ppisvc {@code Constants}
 * ({@code PPI_REQUEST_ID_REGEX}, {@code ALPHANUM_SPACE_REGEX}, …).
 */
public final class ValidationPatterns {

    public static final String ID = "^[a-zA-Z0-9_.-]+$";
    public static final String NAME = "^[a-zA-Z0-9 ]+$";
    public static final String COUPON_CODE = "^[a-zA-Z0-9]+$";

    public static final String ID_MSG = "must match [a-zA-Z0-9_.-] and be at most 50 chars";
    public static final String NAME_MSG = "must be alphanumeric with spaces only";
    public static final String AMOUNT_MSG = "must be a valid positive number with up to 2 decimal places";

    private ValidationPatterns() {
    }
}
