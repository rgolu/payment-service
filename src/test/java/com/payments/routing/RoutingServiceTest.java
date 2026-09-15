package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.RoutingMode;
import com.payments.domain.model.Merchant;
import com.payments.fee.DefaultFeeSchedules;
import com.payments.fee.IdentityFeeMultiplier;
import com.payments.fee.MethodFeePolicy;
import com.payments.provider.ProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RoutingServiceTest {

    private RoutingService routing;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        ProviderRegistry providers = new ProviderRegistry();
        Map<RoutingMode, RoutingStrategy> strategies = new EnumMap<>(RoutingMode.class);
        strategies.put(RoutingMode.FAILOVER, new FailoverRoutingStrategy(providers));
        strategies.put(RoutingMode.CHEAPEST, new CheapestRoutingStrategy(
                providers, new MethodFeePolicy(DefaultFeeSchedules.all(), new IdentityFeeMultiplier())
        ));
        strategies.put(RoutingMode.SUCCESS_RATE, new SuccessRateRoutingStrategy(providers));
        routing = new RoutingService(strategies, RoutingMode.FAILOVER);
        merchant = new Merchant("m1", "Shop", Set.of(PaymentMethod.UPI, PaymentMethod.CARD));
    }

    @Test
    void setMode_Cheapest_SwitchesStrategy() {
        // TEST
        routing.setMode(RoutingMode.CHEAPEST);
        RouteDecision decision = routing.route(PaymentMethod.CARD, merchant);

        // VERIFY
        assertEquals(RoutingMode.CHEAPEST, routing.getMode());
        assertEquals(PaymentMethod.UPI, decision.executedMethod());
    }

    @Test
    void setMode_Unregistered_Throws() {
        RoutingService empty = new RoutingService(new EnumMap<>(RoutingMode.class), RoutingMode.FAILOVER);
        assertThrows(IllegalArgumentException.class, () -> empty.setMode(RoutingMode.CHEAPEST));
    }
}
