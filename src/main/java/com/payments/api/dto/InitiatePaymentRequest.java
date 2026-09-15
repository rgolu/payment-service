package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payments.domain.enums.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import static com.payments.api.ValidationPatterns.AMOUNT_MSG;
import static com.payments.api.ValidationPatterns.COUPON_CODE;
import static com.payments.api.ValidationPatterns.ID;
import static com.payments.api.ValidationPatterns.ID_MSG;

public record InitiatePaymentRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = ID, message = ID_MSG)
        @JsonProperty("payment_id")
        String paymentId,

        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = ID, message = ID_MSG)
        @JsonProperty("user_id")
        String userId,

        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = ID, message = ID_MSG)
        @JsonProperty("merchant_id")
        String merchantId,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 12, fraction = 2, message = AMOUNT_MSG)
        @JsonProperty("amount")
        BigDecimal amount,

        @NotNull
        @JsonProperty("method")
        PaymentMethod method,

        @Size(max = 20)
        @Pattern(regexp = COUPON_CODE, message = "coupon_code must be alphanumeric")
        @JsonProperty("coupon_code")
        String couponCode
) {
}
