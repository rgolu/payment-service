package com.payments.api.dto;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.model.Merchant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record MerchantResponse(
        String id,
        String name,
        Set<PaymentMethod> supportedMethods,
        BigDecimal settlement,
        Instant createdAt
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
