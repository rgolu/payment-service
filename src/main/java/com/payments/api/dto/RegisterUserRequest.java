package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import static com.payments.api.ValidationPatterns.AMOUNT_MSG;
import static com.payments.api.ValidationPatterns.NAME;
import static com.payments.api.ValidationPatterns.NAME_MSG;

public record RegisterUserRequest(
        @NotBlank
        @Size(max = 60)
        @Pattern(regexp = NAME, message = NAME_MSG)
        @JsonProperty("name")
        String name,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 12, fraction = 2, message = AMOUNT_MSG)
        @JsonProperty("initial_balance")
        BigDecimal initialBalance
) {
}
