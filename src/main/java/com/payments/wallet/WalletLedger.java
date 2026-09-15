package com.payments.wallet;

import com.payments.domain.exception.InsufficientBalanceException;
import com.payments.domain.exception.InvalidStateException;
import com.payments.domain.model.Merchant;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Per-user (and per-merchant) locks so two payments cannot overdraw the same wallet.
 * The lock is taken before reading balance and released after the mutation.
 */
public class WalletLedger {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void topUp(User user, Money amount) {
        ReentrantLock lock = lockFor("user:" + user.getId());
        lock.lock();
        try {
            user.credit(amount);
        } finally {
            lock.unlock();
        }
    }

    public void debitUser(User user, Money amount) {
        ReentrantLock lock = lockFor("user:" + user.getId());
        lock.lock();
        try {
            if (user.getWallet().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        "insufficient balance: have " + user.getWallet() + ", need " + amount
                );
            }
            user.debit(amount);
        } finally {
            lock.unlock();
        }
    }

    public void creditUser(User user, Money amount) {
        ReentrantLock lock = lockFor("user:" + user.getId());
        lock.lock();
        try {
            user.credit(amount);
        } finally {
            lock.unlock();
        }
    }

    public void creditMerchant(Merchant merchant, Money amount) {
        ReentrantLock lock = lockFor("merchant:" + merchant.getId());
        lock.lock();
        try {
            merchant.creditSettlement(amount);
        } finally {
            lock.unlock();
        }
    }

    public void debitMerchant(Merchant merchant, Money amount) {
        ReentrantLock lock = lockFor("merchant:" + merchant.getId());
        lock.lock();
        try {
            if (merchant.getSettlement().compareTo(amount) < 0) {
                throw new InvalidStateException(
                        "merchant settlement " + merchant.getSettlement() + " cannot cover refund " + amount
                );
            }
            merchant.debitSettlement(amount);
        } finally {
            lock.unlock();
        }
    }

    private ReentrantLock lockFor(String key) {
        return locks.computeIfAbsent(key, ignored -> new ReentrantLock());
    }
}
