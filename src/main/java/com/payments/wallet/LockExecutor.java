package com.payments.wallet;

import com.payments.domain.exception.ResourceLockedException;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Try-acquire with timeout, never run the critical section without the lock,
 * always release in {@code finally}.
 */
public class LockExecutor {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final Duration timeout;

    public LockExecutor() {
        this(DEFAULT_TIMEOUT);
    }

    public LockExecutor(Duration timeout) {
        this.timeout = timeout;
    }

    public <T> T execute(String key, Supplier<T> operation) {
        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new ResourceLockedException("resource locked: " + key);
            }
            return operation.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResourceLockedException("interrupted waiting for lock: " + key);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void execute(String key, Runnable operation) {
        execute(key, () -> {
            operation.run();
            return null;
        });
    }
}
