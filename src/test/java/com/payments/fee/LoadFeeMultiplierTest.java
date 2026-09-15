package com.payments.fee;

import com.payments.domain.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoadFeeMultiplierTest {

    @Test
    void setLoad_BelowOne_Throws() {
        LoadFeeMultiplier load = new LoadFeeMultiplier();
        assertThrows(IllegalArgumentException.class, () -> load.setLoad(PaymentMethod.UPI, new BigDecimal("0.9")));
    }

    @Test
    void factor_Unset_IsOne() {
        LoadFeeMultiplier load = new LoadFeeMultiplier();
        assertEquals(0, BigDecimal.ONE.compareTo(load.factor(PaymentMethod.UPI)));
        load.setLoad(PaymentMethod.UPI, new BigDecimal("1.20"));
        assertEquals(0, new BigDecimal("1.20").compareTo(load.factor(PaymentMethod.UPI)));
        assertEquals(0, new BigDecimal("1.20").compareTo(load.snapshot().get(PaymentMethod.UPI)));
    }

    @Test
    void identityMultiplier_AlwaysOne() {
        assertEquals(0, BigDecimal.ONE.compareTo(new IdentityFeeMultiplier().factor(PaymentMethod.CARD)));
    }
}
