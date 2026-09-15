package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.ProviderUnavailableException;
import com.payments.domain.model.Merchant;
import com.payments.provider.ProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SuccessRateRoutingStrategyTest {

    private ProviderRegistry providers;
    private SuccessRateRoutingStrategy strategy;
    private Merchant both;

    @BeforeEach
    void setUp() {
        providers = new ProviderRegistry();
        strategy = new SuccessRateRoutingStrategy(providers);
        both = new Merchant("m1", "Shop", Set.of(PaymentMethod.UPI, PaymentMethod.CARD));
    }

    @Test
    void route_DefaultRates_SelectsUpi() {
        // TEST
        RouteDecision decision = strategy.route(PaymentMethod.CARD, both);

        // VERIFY
        assertEquals(PaymentMethod.UPI, decision.executedMethod());
        assertTrue(decision.rerouted());
    }

    @Test
    void route_CardHigherSuccessRate_SelectsCard() {
        // GIVEN
        providers.setSuccessRate(PaymentMethod.CARD, 0.99);
        providers.setSuccessRate(PaymentMethod.UPI, 0.90);

        // TEST
        RouteDecision decision = strategy.route(PaymentMethod.UPI, both);

        // VERIFY
        assertEquals(PaymentMethod.CARD, decision.executedMethod());
        assertTrue(decision.rerouted());
    }

    @Test
    void route_RequestedAlreadyBest_NotRerouted() {
        RouteDecision decision = strategy.route(PaymentMethod.UPI, both);
        assertEquals(PaymentMethod.UPI, decision.executedMethod());
        assertFalse(decision.rerouted());
    }

    @Test
    void route_NoProviderUp_Throws() {
        providers.setAvailable(PaymentMethod.UPI, false);
        providers.setAvailable(PaymentMethod.CARD, false);
        assertThrows(ProviderUnavailableException.class, () -> strategy.route(PaymentMethod.UPI, both));
    }
}
