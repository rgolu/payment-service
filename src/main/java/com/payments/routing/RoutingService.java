package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.RoutingMode;
import com.payments.domain.model.Merchant;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds the active routing strategy. PaymentService talks only to this type,
 * so swapping FAILOVER / CHEAPEST / SUCCESS_RATE never touches debit/credit logic.
 */
public class RoutingService {

    private final Map<RoutingMode, RoutingStrategy> strategies;
    private volatile RoutingMode mode;

    public RoutingService(Map<RoutingMode, RoutingStrategy> strategies, RoutingMode initial) {
        this.strategies = new EnumMap<>(strategies);
        this.mode = initial;
    }

    public RouteDecision route(PaymentMethod requested, Merchant merchant) {
        return strategies.get(mode).route(requested, merchant);
    }

    public RoutingMode getMode() {
        return mode;
    }

    public void setMode(RoutingMode mode) {
        if (!strategies.containsKey(mode)) {
            throw new IllegalArgumentException("no strategy registered for " + mode);
        }
        this.mode = mode;
    }
}
