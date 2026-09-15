package com.payments.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegisterUserRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.00") BigDecimal initialBalance
) {
}
