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
import static com.payments.api.ValidationPatterns.ID;
import static com.payments.api.ValidationPatterns.ID_MSG;

public record TopUpRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = ID, message = ID_MSG)
        @JsonProperty("credit_id")
        String creditId,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 12, fraction = 2, message = AMOUNT_MSG)
        @JsonProperty("amount")
        BigDecimal amount
) {
}
