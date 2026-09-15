package com.payments.domain.model;

import com.payments.domain.enums.CouponType;
import com.payments.domain.money.Money;

import java.time.Instant;

public class Coupon {

    private final String code;
    private final CouponType type;
    private final int value;
    private final Money minAmount;
    private final Money maxDiscount;
    private Integer remainingUses;
    private final Instant expiresAt;
    private boolean active;

    public Coupon(
            String code,
            CouponType type,
            int value,
            Money minAmount,
            Money maxDiscount,
            Integer remainingUses,
            Instant expiresAt
    ) {
        this.code = code.toUpperCase();
        this.type = type;
        this.value = value;
        this.minAmount = minAmount == null ? Money.ZERO : minAmount;
        this.maxDiscount = maxDiscount;
        this.remainingUses = remainingUses;
        this.expiresAt = expiresAt;
        this.active = true;
    }

    public String getCode() {
        return code;
    }

    public CouponType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public Money getMinAmount() {
        return minAmount;
    }

    public Money getMaxDiscount() {
        return maxDiscount;
    }

    public Integer getRemainingUses() {
        return remainingUses;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void consumeUse() {
        if (remainingUses != null) {
            remainingUses = remainingUses - 1;
        }
    }
}
