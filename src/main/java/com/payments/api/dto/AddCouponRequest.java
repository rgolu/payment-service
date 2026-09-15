package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payments.domain.enums.CouponType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

import static com.payments.api.ValidationPatterns.AMOUNT_MSG;
import static com.payments.api.ValidationPatterns.COUPON_CODE;

/**
 * {@code value} is a percent for PERCENT (10 = 10%) and rupees for FLAT (100 = ₹100).
 */
public record AddCouponRequest(
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = COUPON_CODE, message = "code must be alphanumeric")
        @JsonProperty("code")
        String code,

        @NotNull
        @JsonProperty("type")
        CouponType type,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 12, fraction = 2, message = AMOUNT_MSG)
        @JsonProperty("value")
        BigDecimal value,

        @Digits(integer = 12, fraction = 2, message = AMOUNT_MSG)
        @JsonProperty("min_amount")
        BigDecimal minAmount,

        @Digits(integer = 12, fraction = 2, message = AMOUNT_MSG)
        @JsonProperty("max_discount")
        BigDecimal maxDiscount,

        @JsonProperty("remaining_uses")
        Integer remainingUses,

        @JsonProperty("expires_at")
        Instant expiresAt
) {
}
