package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record AvailabilityRequest(
        @NotNull @JsonProperty("available") Boolean available
) {
}
