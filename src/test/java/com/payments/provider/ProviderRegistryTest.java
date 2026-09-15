package com.payments.provider;

import com.payments.domain.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProviderRegistryTest {

    @Test
    void defaults_UpiAndCardUp() {
        ProviderRegistry registry = new ProviderRegistry();
        assertTrue(registry.isAvailable(PaymentMethod.UPI));
        assertTrue(registry.isAvailable(PaymentMethod.CARD));
        assertEquals(0.97, registry.successRate(PaymentMethod.UPI));
        assertEquals(0.93, registry.successRate(PaymentMethod.CARD));
        assertTrue(registry.availabilitySnapshot().get(PaymentMethod.UPI));
    }

    @Test
    void setSuccessRate_OutOfRange_Throws() {
        ProviderRegistry registry = new ProviderRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.setSuccessRate(PaymentMethod.UPI, 1.1));
        assertThrows(IllegalArgumentException.class, () -> registry.setSuccessRate(PaymentMethod.UPI, -0.1));
    }

    @Test
    void register_OverridesAvailabilityAndRate() {
        ProviderRegistry registry = new ProviderRegistry();
        registry.register(PaymentMethod.CARD, false, 0.5);
        assertFalse(registry.isAvailable(PaymentMethod.CARD));
        assertEquals(0.5, registry.successRate(PaymentMethod.CARD));
    }
}
