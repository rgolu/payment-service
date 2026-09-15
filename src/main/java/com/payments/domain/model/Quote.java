package com.payments.domain.model;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.money.Money;

/**
 * Immutable price breakdown captured at initiate time.
 */
public record Quote(
        Money originalAmount,
        Money discount,
        Money principal,
        Money fee,
        Money total,
        PaymentMethod requestedMethod,
        PaymentMethod executedMethod,
        PaymentMethod feeMethod,
        boolean rerouted,
        String couponCode
) {
}
