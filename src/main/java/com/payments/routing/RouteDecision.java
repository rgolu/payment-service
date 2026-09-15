package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;

public record RouteDecision(
        PaymentMethod executedMethod,
        PaymentMethod feeMethod,
        boolean rerouted,
        String reason
) {
}
