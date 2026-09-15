package com.payments.domain.model;

import com.payments.domain.money.Money;

import java.time.Instant;

public class User {

    private final String id;
    private final String name;
    private Money wallet;
    private final Instant createdAt;

    public User(String id, String name, Money wallet) {
        this.id = id;
        this.name = name;
        this.wallet = wallet;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public synchronized Money getWallet() {
        return wallet;
    }

    public synchronized void credit(Money amount) {
        this.wallet = this.wallet.plus(amount);
    }

    public synchronized void debit(Money amount) {
        if (wallet.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient wallet balance");
        }
        this.wallet = this.wallet.minus(amount);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
