package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

import static com.payments.api.ValidationPatterns.AMOUNT_MSG;

public record LoadRequest(
        @NotNull
        @DecimalMin("1.0")
        @Digits(integer = 4, fraction = 2, message = AMOUNT_MSG)
        @JsonProperty("factor")
        BigDecimal factor
) {
}
