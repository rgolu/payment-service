package com.payments.fee;

import com.payments.domain.enums.PaymentMethod;

import java.math.BigDecimal;

public class IdentityFeeMultiplier implements FeeMultiplier {

    @Override
    public BigDecimal factor(PaymentMethod method) {
        return BigDecimal.ONE;
    }
}
