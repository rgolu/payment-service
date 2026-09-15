package com.payments.refund;

import com.payments.domain.money.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RefundPolicyTest {

    private final RefundPolicy policy = new RefundPolicy();

    @Test
    void refundFee_Thousand_IsOnePercent() {
        assertEquals(Money.rupees("10"), policy.refundFee(Money.rupees("1000")));
    }

    @Test
    void refundFee_SmallPrincipal_FloorsToTwo() {
        assertEquals(Money.rupees("2"), policy.refundFee(Money.rupees("100")));
    }

    @Test
    void refundFee_TinyPrincipal_CappedAtPrincipal() {
        assertEquals(Money.rupees("1"), policy.refundFee(Money.rupees("1")));
    }
}
