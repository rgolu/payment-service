package com.payments.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IdGeneratorTest {

    @Test
    void generatedIds_HaveExpectedPrefixes() {
        IdGenerator ids = new IdGenerator();
        assertTrue(ids.userId().startsWith("usr_"));
        assertTrue(ids.merchantId().startsWith("mer_"));
        assertTrue(ids.paymentId().startsWith("pay_"));
    }
}
