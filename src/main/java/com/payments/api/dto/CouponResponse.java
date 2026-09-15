package com.payments.api.dto;

import com.payments.domain.enums.CouponType;
import com.payments.domain.model.Coupon;
import com.payments.domain.money.Money;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        String code,
        CouponType type,
        BigDecimal value,
        BigDecimal minAmount,
        BigDecimal maxDiscount,
        Integer remainingUses,
        Instant expiresAt,
        boolean active
) {

    public static CouponResponse from(Coupon coupon) {
        BigDecimal displayValue = coupon.getType() == CouponType.FLAT
                ? Money.ofPaise(coupon.getValue()).rupees()
                : BigDecimal.valueOf(coupon.getValue());
        return new CouponResponse(
                coupon.getCode(),
                coupon.getType(),
                displayValue,
                coupon.getMinAmount().rupees(),
                coupon.getMaxDiscount() == null ? null : coupon.getMaxDiscount().rupees(),
                coupon.getRemainingUses(),
                coupon.getExpiresAt(),
                coupon.isActive()
        );
    }
}
