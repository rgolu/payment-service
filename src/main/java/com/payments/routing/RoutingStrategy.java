package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.model.Merchant;

public interface RoutingStrategy {

    RouteDecision route(PaymentMethod requested, Merchant merchant);
}
