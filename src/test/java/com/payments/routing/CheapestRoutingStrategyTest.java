package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.ProviderUnavailableException;
import com.payments.domain.model.Merchant;
import com.payments.fee.DefaultFeeSchedules;
import com.payments.fee.IdentityFeeMultiplier;
import com.payments.fee.MethodFeePolicy;
import com.payments.provider.ProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheapestRoutingStrategyTest {

    private ProviderRegistry providers;
    private CheapestRoutingStrategy strategy;
    private Merchant both;

    @BeforeEach
    void setUp() {
        providers = new ProviderRegistry();
        strategy = new CheapestRoutingStrategy(
                providers, new MethodFeePolicy(DefaultFeeSchedules.all(), new IdentityFeeMultiplier())
        );
        both = new Merchant("m1", "Shop", Set.of(PaymentMethod.UPI, PaymentMethod.CARD));
    }

    @Test
    void route_BothUp_SelectsUpiAsCheapest() {
        // TEST
        RouteDecision decision = strategy.route(PaymentMethod.CARD, both);

        // VERIFY
        assertEquals(PaymentMethod.UPI, decision.executedMethod());
        assertEquals(PaymentMethod.UPI, decision.feeMethod());
        assertTrue(decision.rerouted());
    }

    @Test
    void route_RequestedAlreadyCheapest_NotRerouted() {
        // TEST
        RouteDecision decision = strategy.route(PaymentMethod.UPI, both);

        // VERIFY
        assertEquals(PaymentMethod.UPI, decision.executedMethod());
        assertFalse(decision.rerouted());
    }

    @Test
    void route_NoProviderUp_Throws() {
        // GIVEN
        providers.setAvailable(PaymentMethod.UPI, false);
        providers.setAvailable(PaymentMethod.CARD, false);

        // TEST + VERIFY
        assertThrows(ProviderUnavailableException.class, () -> strategy.route(PaymentMethod.UPI, both));
    }
}
