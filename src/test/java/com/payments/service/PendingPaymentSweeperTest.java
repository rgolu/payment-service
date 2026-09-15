package com.payments.service;

import com.payments.TestHarness;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.PaymentStatus;
import com.payments.domain.model.Merchant;
import com.payments.domain.model.Payment;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PendingPaymentSweeperTest {

    @Test
    void sweep_ExpiresStalePending() {
        TestHarness h = new TestHarness();
        User user = h.users.register("Rishav", Money.rupees("1000"));
        Merchant merchant = h.merchants.register("Cafe", Set.of(PaymentMethod.UPI));
        Payment pending = h.payments.initiate(
                "pay_stale", user.getId(), merchant.getId(), Money.rupees("100"), PaymentMethod.UPI, null
        );
        h.clock.advance(Duration.ofMinutes(15));

        new PendingPaymentSweeper(h.payments).sweep();

        assertEquals(PaymentStatus.EXPIRED, h.payments.get(pending.getId()).getStatus());
        assertEquals(Money.rupees("1000"), user.getWallet());
    }
}
