package com.payments.service;

import com.payments.TestHarness;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.exception.NotFoundException;
import com.payments.domain.exception.PaymentException;
import com.payments.domain.model.Merchant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.payments.common.TestConstants.MERCHANT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MerchantServiceTest {

    private TestHarness h;

    @BeforeEach
    void setUp() {
        h = new TestHarness();
    }

    @Test
    void register_WithMethods_CreatesMerchant() {
        Merchant merchant = h.merchants.register(MERCHANT_NAME, Set.of(PaymentMethod.UPI));
        assertEquals(MERCHANT_NAME, merchant.getName());
        assertTrue(merchant.getId().startsWith("mer_"));
        assertTrue(merchant.supports(PaymentMethod.UPI));
    }

    @Test
    void register_EmptyMethods_Throws() {
        assertThrows(PaymentException.class, () -> h.merchants.register(MERCHANT_NAME, Set.of()));
    }

    @Test
    void get_UnknownMerchant_Throws() {
        assertThrows(NotFoundException.class, () -> h.merchants.get("mer_missing"));
    }
}
