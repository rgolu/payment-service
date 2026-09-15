package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payments.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

import static com.payments.api.ValidationPatterns.NAME;
import static com.payments.api.ValidationPatterns.NAME_MSG;

public record RegisterMerchantRequest(
        @NotBlank
        @Size(max = 60)
        @Pattern(regexp = NAME, message = NAME_MSG)
        @JsonProperty("name")
        String name,

        @NotEmpty
        @JsonProperty("supported_methods")
        Set<PaymentMethod> supportedMethods
) {
}
