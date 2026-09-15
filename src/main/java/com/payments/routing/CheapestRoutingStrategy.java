package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.ProviderUnavailableException;
import com.payments.domain.model.Merchant;
import com.payments.domain.money.Money;
import com.payments.fee.FeePolicy;
import com.payments.provider.ProviderRegistry;

/**
 * Bonus: among methods the merchant supports and that are up, pick the cheapest
 * fee for a representative ₹1,000. User-requested method is ignored for rail
 * choice; feeMethod follows the chosen rail (this is an explicit trade-off).
 */
public class CheapestRoutingStrategy implements RoutingStrategy {

    private static final Money SAMPLE = Money.rupees("1000");

    private final ProviderRegistry providers;
    private final FeePolicy feePolicy;

    public CheapestRoutingStrategy(ProviderRegistry providers, FeePolicy feePolicy) {
        this.providers = providers;
        this.feePolicy = feePolicy;
    }

    @Override
    public RouteDecision route(PaymentMethod requested, Merchant merchant) {
        PaymentMethod best = null;
        Money bestFee = null;
        for (PaymentMethod method : merchant.getSupportedMethods()) {
            if (!providers.isAvailable(method)) {
                continue;
            }
            Money fee = feePolicy.quoteFee(SAMPLE, method);
            if (bestFee == null || fee.compareTo(bestFee) < 0) {
                best = method;
                bestFee = fee;
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
                rerouted ? "cheapest rail selected: " + best : null
        );
    }
}
