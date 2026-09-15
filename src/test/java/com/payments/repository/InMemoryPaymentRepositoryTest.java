package com.payments.repository;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.model.Payment;
import com.payments.domain.model.Quote;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryPaymentRepositoryTest {

    @Test
    void save_UpdatesPendingIndexOnComplete() {
        InMemoryPaymentRepository repo = new InMemoryPaymentRepository();
        Instant now = Instant.parse("2026-09-15T00:00:00Z");
        Quote quote = new Quote(
                Money.rupees("100"), Money.ZERO, Money.rupees("100"), Money.rupees("5"),
                Money.rupees("105"), PaymentMethod.UPI, PaymentMethod.UPI, PaymentMethod.UPI, false, null
        );
        Payment payment = new Payment("pay_1", "usr_1", "mer_1", quote, now, now.plusSeconds(60));
        repo.save(payment);
        assertEquals(1, repo.findPending().size());
        assertEquals("pay_1", repo.findByUserId("usr_1").get(0).getId());
        assertEquals("pay_1", repo.findByMerchantId("mer_1").get(0).getId());
        assertTrue(repo.findByUserId("usr_none").isEmpty());

        payment.markCompleted(now);
        repo.save(payment);
        assertTrue(repo.findPending().isEmpty());
        assertTrue(repo.findById("pay_1").isPresent());
    }
}
