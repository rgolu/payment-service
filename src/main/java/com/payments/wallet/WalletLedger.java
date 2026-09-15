package com.payments.wallet;

import com.payments.domain.exception.InsufficientBalanceException;
import com.payments.domain.exception.InvalidStateException;
import com.payments.domain.model.Merchant;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;

/**
 * Wallet mutations always run inside {@link LockExecutor}. Caller may also
 * hold an outer idempotency lock; lock order is {@code idemp:*} then {@code user:*}
 * / {@code merchant:*} so we never deadlock.
 */
public class WalletLedger {

    private final LockExecutor locks;

    public WalletLedger(LockExecutor locks) {
        this.locks = locks;
    }

    public void topUp(User user, Money amount) {
        locks.execute("user:" + user.getId(), () -> user.credit(amount));
    }

    public void debitUser(User user, Money amount) {
        locks.execute("user:" + user.getId(), () -> {
            if (user.getWallet().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                        "insufficient balance: have " + user.getWallet() + ", need " + amount
                );
            }
            user.debit(amount);
        });
    }

    public void creditUser(User user, Money amount) {
        locks.execute("user:" + user.getId(), () -> user.credit(amount));
    }

    public void creditMerchant(Merchant merchant, Money amount) {
        locks.execute("merchant:" + merchant.getId(), () -> merchant.creditSettlement(amount));
    }

    public void debitMerchant(Merchant merchant, Money amount) {
        locks.execute("merchant:" + merchant.getId(), () -> {
            if (merchant.getSettlement().compareTo(amount) < 0) {
                throw new InvalidStateException(
                        "merchant settlement " + merchant.getSettlement() + " cannot cover refund " + amount
                );
            }
            merchant.debitSettlement(amount);
        });
    }
}
