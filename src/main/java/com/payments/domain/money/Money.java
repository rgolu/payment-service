package com.payments.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable INR amount stored as integer paise so fee math never uses floating point.
 */
public final class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(0);

    private final long paise;

    private Money(long paise) {
        if (paise < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
        this.paise = paise;
    }

    public static Money ofPaise(long paise) {
        return new Money(paise);
    }

    public static Money rupees(String amount) {
        return rupees(new BigDecimal(amount));
    }

    public static Money rupees(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
        long paise = amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        return new Money(paise);
    }

    public long paise() {
        return paise;
    }

    public BigDecimal rupees() {
        return BigDecimal.valueOf(paise, 2);
    }

    public Money plus(Money other) {
        return new Money(this.paise + other.paise);
    }

    public Money minus(Money other) {
        if (this.paise < other.paise) {
            throw new IllegalArgumentException("subtraction would be negative");
        }
        return new Money(this.paise - other.paise);
    }

    /**
     * Apply a percent (2 means 2%). Rounded half-up to the nearest paisa.
     */
    public Money percent(int rate) {
        return ofPaise((this.paise * rate + 50) / 100);
    }

    /**
     * Apply a rate in basis points (200 = 2%). Rounded half-up to the nearest paisa.
     */
    public Money mulBps(int bps) {
        return ofPaise((this.paise * bps + 5_000) / 10_000);
    }

    /**
     * Multiply by a load factor (1.2 = +20%). Rounded half-up.
     */
    public Money times(BigDecimal factor) {
        BigDecimal product = BigDecimal.valueOf(paise)
                .multiply(factor)
                .setScale(0, RoundingMode.HALF_UP);
        return ofPaise(product.longValueExact());
    }

    public boolean isZero() {
        return paise == 0;
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(this.paise, other.paise);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return paise == money.paise;
    }

    @Override
    public int hashCode() {
        return Objects.hash(paise);
    }

    @Override
    public String toString() {
        return "₹" + rupees().toPlainString();
    }
}
