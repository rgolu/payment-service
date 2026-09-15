package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.PaymentException;
import com.payments.domain.exception.ProviderUnavailableException;
import com.payments.domain.model.Merchant;
import com.payments.provider.ProviderRegistry;

/**
 * Spec default: honour the requested method when it is up and the merchant
 * supports it. If UPI is down, fall back to Card and keep pricing the UPI schedule
 * so the user pays no extra fee.
 */
public class FailoverRoutingStrategy implements RoutingStrategy {

    private final ProviderRegistry providers;

    public FailoverRoutingStrategy(ProviderRegistry providers) {
        this.providers = providers;
    }

    @Override
    public RouteDecision route(PaymentMethod requested, Merchant merchant) {
        if (!merchant.supports(requested)) {
            throw new PaymentException("merchant does not support " + requested);
        }

        if (providers.isAvailable(requested)) {
            return new RouteDecision(requested, requested, false, null);
        }

        if (requested == PaymentMethod.UPI) {
            if (!merchant.supports(PaymentMethod.CARD)) {
                throw new PaymentException("UPI unavailable and merchant does not support CARD fallback");
            }
            if (!providers.isAvailable(PaymentMethod.CARD)) {
                throw new ProviderUnavailableException("UPI and CARD providers are both down");
            }
            return new RouteDecision(
                    PaymentMethod.CARD,
                    PaymentMethod.UPI,
                    true,
                    "UPI provider down; routed via CARD at UPI fee"
            );
        }

        throw new ProviderUnavailableException(requested + " provider is down and no fallback applies");
    }
}
