package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.payments.api.ValidationPatterns.ID;
import static com.payments.api.ValidationPatterns.ID_MSG;

public record RefundRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = ID, message = ID_MSG)
        @JsonProperty("refund_id")
        String refundId
) {
}
