package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payments.domain.enums.CouponType;
import com.payments.domain.model.Coupon;
import com.payments.domain.money.Money;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponse(
        @JsonProperty("code") String code,
        @JsonProperty("type") CouponType type,
        @JsonProperty("value") BigDecimal value,
        @JsonProperty("min_amount") BigDecimal minAmount,
        @JsonProperty("max_discount") BigDecimal maxDiscount,
        @JsonProperty("remaining_uses") Integer remainingUses,
        @JsonProperty("expires_at") Instant expiresAt,
        @JsonProperty("active") boolean active
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
