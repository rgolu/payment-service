package com.payments.api.dto;

import com.payments.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RegisterMerchantRequest(
        @NotBlank String name,
        @NotEmpty Set<PaymentMethod> supportedMethods
) {
}
