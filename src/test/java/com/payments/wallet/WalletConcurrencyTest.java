package com.payments.wallet;

import com.payments.TestHarness;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.model.Merchant;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WalletConcurrencyTest {

    @Test
    void concurrentInitiatesCannotOverdrawTheSameWallet() throws Exception {
        TestHarness h = new TestHarness();
        User user = h.users.register("Racer", Money.rupees("100"));
        Merchant merchant = h.merchants.register("Shop", Set.of(PaymentMethod.UPI, PaymentMethod.CARD));

        int threads = 10;
        // Each attempt charges ₹20 + ₹5 min fee = ₹25. Wallet ₹100 can fund exactly 4.
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    h.payments.initiate(
                            user.getId(), merchant.getId(), Money.rupees("20"), PaymentMethod.UPI, null
                    );
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    failures.incrementAndGet();
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(successes.get()).isEqualTo(4);
        assertThat(failures.get()).isEqualTo(6);
        assertThat(user.getWallet().paise()).isZero();
        assertThat(h.payments.historyForUser(user.getId())).hasSize(4);
    }
}
