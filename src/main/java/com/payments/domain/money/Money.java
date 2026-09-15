package com.payments.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * INR amount as {@link BigDecimal} scaled to exactly 2 decimal places (HALF_UP).
 * Request validation uses {@code @Digits(integer = 12, fraction = 2)}.
 */
public final class Money implements Comparable<Money> {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final Money ZERO = of(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    public static Money of(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("amount is required");
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
        return new Money(value.setScale(SCALE, ROUNDING));
    }

    public static Money rupees(String amount) {
        return of(new BigDecimal(amount));
    }

    public static Money rupees(BigDecimal amount) {
        return of(amount);
    }

    /** Test/legacy helper: 100 paise = ₹1.00. */
    public static Money ofPaise(long paise) {
        if (paise < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
        return of(BigDecimal.valueOf(paise, SCALE));
    }

    public BigDecimal amount() {
        return amount;
    }

    public BigDecimal rupees() {
        return amount;
    }

    public long paise() {
        return amount.movePointRight(SCALE).longValueExact();
    }

    public Money plus(Money other) {
        return of(this.amount.add(other.amount));
    }

    public Money minus(Money other) {
        if (this.amount.compareTo(other.amount) < 0) {
            throw new IllegalArgumentException("subtraction would be negative");
        }
        return of(this.amount.subtract(other.amount));
    }

    /** {@code rate} is a percent (2 means 2%). */
    public Money percent(int rate) {
        return of(amount.multiply(BigDecimal.valueOf(rate))
                .divide(BigDecimal.valueOf(100), SCALE, ROUNDING));
    }

    /** Basis points (200 = 2%). */
    public Money mulBps(int bps) {
        return of(amount.multiply(BigDecimal.valueOf(bps))
                .divide(BigDecimal.valueOf(10_000), SCALE, ROUNDING));
    }

    public Money times(BigDecimal factor) {
        return of(amount.multiply(factor));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return "₹" + amount.toPlainString();
    }
}
