package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.ProviderUnavailableException;
import com.payments.domain.model.Merchant;
import com.payments.provider.ProviderRegistry;

/** Bonus: pick the highest-success-rate rail that is up and merchant-supported. */
public class SuccessRateRoutingStrategy implements RoutingStrategy {

    private final ProviderRegistry providers;

    public SuccessRateRoutingStrategy(ProviderRegistry providers) {
        this.providers = providers;
    }

    @Override
    public RouteDecision route(PaymentMethod requested, Merchant merchant) {
        PaymentMethod best = null;
        double bestRate = -1;
        for (PaymentMethod method : merchant.getSupportedMethods()) {
            if (!providers.isAvailable(method)) {
                continue;
            }
            double rate = providers.successRate(method);
            if (rate > bestRate) {
                best = method;
                bestRate = rate;
            }
        }
        if (best == null) {
            throw new ProviderUnavailableException("no available provider for merchant " + merchant.getId());
        }
        boolean rerouted = best != requested;
        return new RouteDecision(
                best,
                best,
                rerouted,
                rerouted ? "highest success-rate rail selected: " + best : null
        );
    }
}
