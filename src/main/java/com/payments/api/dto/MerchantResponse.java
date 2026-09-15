package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.model.Merchant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record MerchantResponse(
        @JsonProperty("merchant_id") String merchantId,
        @JsonProperty("name") String name,
        @JsonProperty("supported_methods") Set<PaymentMethod> supportedMethods,
        @JsonProperty("settlement") BigDecimal settlement,
        @JsonProperty("created_at") Instant createdAt
) {

    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getName(),
                merchant.getSupportedMethods(),
                merchant.getSettlement().rupees(),
                merchant.getCreatedAt()
        );
    }
}
