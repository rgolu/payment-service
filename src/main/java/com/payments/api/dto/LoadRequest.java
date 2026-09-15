package com.payments.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoadRequest(@NotNull @DecimalMin("1.0") BigDecimal factor) {
}
