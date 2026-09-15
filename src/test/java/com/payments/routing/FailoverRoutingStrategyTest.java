package com.payments.routing;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.PaymentException;
import com.payments.domain.exception.ProviderUnavailableException;
import com.payments.domain.model.Merchant;
import com.payments.provider.ProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailoverRoutingStrategyTest {

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
    void usesRequestedMethodWhenUp() {
        RouteDecision decision = strategy.route(PaymentMethod.UPI, both);
        assertThat(decision.executedMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(decision.feeMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(decision.rerouted()).isFalse();
    }

    @Test
    void reroutesUpiToCardAtUpiFeeWhenProviderDown() {
        providers.setAvailable(PaymentMethod.UPI, false);
        RouteDecision decision = strategy.route(PaymentMethod.UPI, both);
        assertThat(decision.executedMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(decision.feeMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(decision.rerouted()).isTrue();
    }

    @Test
    void doesNotRerouteWhenMerchantLacksCard() {
        providers.setAvailable(PaymentMethod.UPI, false);
        assertThatThrownBy(() -> strategy.route(PaymentMethod.UPI, upiOnly))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void rejectsUnsupportedMethod() {
        Merchant cardOnly = new Merchant("m3", "Cards", Set.of(PaymentMethod.CARD));
        assertThatThrownBy(() -> strategy.route(PaymentMethod.UPI, cardOnly))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void cardDowntimeHasNoFallback() {
        providers.setAvailable(PaymentMethod.CARD, false);
        assertThatThrownBy(() -> strategy.route(PaymentMethod.CARD, both))
                .isInstanceOf(ProviderUnavailableException.class);
    }
}
