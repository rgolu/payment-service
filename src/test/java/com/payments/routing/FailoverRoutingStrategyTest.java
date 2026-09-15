package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.PaymentException;
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

public class FailoverRoutingStrategyTest {

    private ProviderRegistry providers;
    private FailoverRoutingStrategy strategy;
    private Merchant both;
    private Merchant upiOnly;

    @BeforeEach
    void setUp() {
        providers = new ProviderRegistry();
        strategy = new FailoverRoutingStrategy(providers);
        both = new Merchant("m1", "Shop", Set.of(PaymentMethod.UPI, PaymentMethod.CARD));
        upiOnly = new Merchant("m2", "UpiShop", Set.of(PaymentMethod.UPI));
    }

    @Test
    void route_RequestedMethodUp_UsesRequestedRail() {
        // TEST
        RouteDecision decision = strategy.route(PaymentMethod.UPI, both);

        // VERIFY
        assertEquals(PaymentMethod.UPI, decision.executedMethod());
        assertEquals(PaymentMethod.UPI, decision.feeMethod());
        assertFalse(decision.rerouted());
    }

    @Test
    void route_UpiDown_ReroutesToCardAtUpiFee() {
        // GIVEN
        providers.setAvailable(PaymentMethod.UPI, false);

        // TEST
        RouteDecision decision = strategy.route(PaymentMethod.UPI, both);

        // VERIFY
        assertEquals(PaymentMethod.CARD, decision.executedMethod());
        assertEquals(PaymentMethod.UPI, decision.feeMethod());
        assertTrue(decision.rerouted());
    }

    @Test
    void route_UpiDownMerchantLacksCard_Throws() {
        // GIVEN
        providers.setAvailable(PaymentMethod.UPI, false);

        // TEST + VERIFY
        assertThrows(PaymentException.class, () -> strategy.route(PaymentMethod.UPI, upiOnly));
    }

    @Test
    void route_UnsupportedMethod_Throws() {
        // GIVEN
        Merchant cardOnly = new Merchant("m3", "Cards", Set.of(PaymentMethod.CARD));

        // TEST + VERIFY
        assertThrows(PaymentException.class, () -> strategy.route(PaymentMethod.UPI, cardOnly));
    }

    @Test
    void route_CardDown_HasNoFallback() {
        // GIVEN
        providers.setAvailable(PaymentMethod.CARD, false);

        // TEST + VERIFY
        assertThrows(ProviderUnavailableException.class, () -> strategy.route(PaymentMethod.CARD, both));
    }

    @Test
    void route_UpiAndCardDown_Throws() {
        // GIVEN
        providers.setAvailable(PaymentMethod.UPI, false);
        providers.setAvailable(PaymentMethod.CARD, false);

        // TEST + VERIFY
        assertThrows(ProviderUnavailableException.class, () -> strategy.route(PaymentMethod.UPI, both));
    }
}
