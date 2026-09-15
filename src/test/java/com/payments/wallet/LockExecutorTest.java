package com.payments.wallet;

import com.payments.domain.exception.ResourceLockedException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LockExecutorTest {

    @Test
    void execute_Supplier_ReturnsValue() {
        LockExecutor locks = new LockExecutor();
        assertEquals(7, locks.execute("k", () -> 7));
    }

    @Test
    void execute_Timeout_ThrowsResourceLocked() throws Exception {
        LockExecutor locks = new LockExecutor(Duration.ofMillis(50));
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> locks.execute("hot", () -> {
            held.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        holder.start();
        held.await();

        assertThrows(ResourceLockedException.class, () -> locks.execute("hot", () -> "nope"));

        release.countDown();
        holder.join();
    }

    @Test
    void execute_InterruptedWhileWaiting_SetsInterruptFlag() throws Exception {
        LockExecutor locks = new LockExecutor(Duration.ofSeconds(5));
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> locks.execute("hot", () -> {
            held.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        holder.start();
        held.await();

        AtomicBoolean locked = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            try {
                locks.execute("hot", () -> "x");
            } catch (ResourceLockedException ex) {
                locked.set(true);
            }
        });
        waiter.start();
        Thread.sleep(20);
        waiter.interrupt();
        waiter.join();
        assertTrue(locked.get());

        release.countDown();
        holder.join();
    }
}
