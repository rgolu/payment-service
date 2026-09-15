package com.payments.domain.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoneyTest {

    @Test
    void of_Null_Throws() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(null));
    }

    @Test
    void of_Negative_Throws() {
        assertThrows(IllegalArgumentException.class, () -> Money.rupees("-1"));
        assertThrows(IllegalArgumentException.class, () -> Money.ofPaise(-1));
    }

    @Test
    void minus_WouldBeNegative_Throws() {
        assertThrows(IllegalArgumentException.class, () -> Money.rupees("1").minus(Money.rupees("2")));
    }

    @Test
    void plusMinusPercentMulBpsTimes_ComputeScaleTwo() {
        Money amount = Money.rupees("100.00");
        assertEquals(Money.rupees("110.00"), amount.plus(Money.rupees("10")));
        assertEquals(Money.rupees("90.00"), amount.minus(Money.rupees("10")));
        assertEquals(Money.rupees("2.00"), amount.percent(2));
        assertEquals(Money.rupees("2.00"), amount.mulBps(200));
        assertEquals(Money.rupees("120.00"), amount.times(new BigDecimal("1.20")));
        assertEquals(10000, amount.paise());
        assertEquals(new BigDecimal("100.00"), amount.amount());
        assertTrue(Money.ZERO.isZero());
        assertFalse(amount.isZero());
    }

    @Test
    void equals_SameScale_Equal() {
        assertEquals(Money.rupees("10"), Money.rupees(new BigDecimal("10.00")));
        assertEquals(Money.rupees("10").hashCode(), Money.rupees("10.00").hashCode());
        assertNotEquals(Money.rupees("10"), Money.rupees("11"));
        assertNotEquals(Money.rupees("10"), "10");
        assertEquals("₹10.00", Money.rupees("10").toString());
    }
}
