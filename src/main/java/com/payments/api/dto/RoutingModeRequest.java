package com.payments.api.dto;

import com.payments.domain.enums.RoutingMode;
import jakarta.validation.constraints.NotNull;

public record RoutingModeRequest(@NotNull RoutingMode mode) {
}
