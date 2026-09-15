package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payments.domain.enums.RoutingMode;
import jakarta.validation.constraints.NotNull;

public record RoutingModeRequest(
        @NotNull @JsonProperty("mode") RoutingMode mode
) {
}
