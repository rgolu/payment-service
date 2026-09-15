package com.payments.api.dto;

import com.payments.domain.enums.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * {@code value} is a percent for PERCENT (10 = 10%) and rupees for FLAT (100 = ₹100).
 */
public record AddCouponRequest(
        @NotBlank String code,
        @NotNull CouponType type,
        @NotNull @DecimalMin("0.01") BigDecimal value,
        BigDecimal minAmount,
        BigDecimal maxDiscount,
        Integer remainingUses,
        Instant expiresAt
) {
}
