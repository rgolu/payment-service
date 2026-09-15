package com.payments.domain.model;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.money.Money;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class Merchant {

    private final String id;
    private final String name;
    private final Set<PaymentMethod> supportedMethods;
    private Money settlement;
    private final Instant createdAt;

    public Merchant(String id, String name, Set<PaymentMethod> supportedMethods) {
        this.id = id;
        this.name = name;
        this.supportedMethods = Collections.unmodifiableSet(EnumSet.copyOf(supportedMethods));
        this.settlement = Money.ZERO;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<PaymentMethod> getSupportedMethods() {
        return supportedMethods;
    }

    public boolean supports(PaymentMethod method) {
        return supportedMethods.contains(method);
    }

    public synchronized Money getSettlement() {
        return settlement;
    }

    public synchronized void creditSettlement(Money amount) {
        this.settlement = this.settlement.plus(amount);
    }

    public synchronized void debitSettlement(Money amount) {
        if (settlement.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient settlement balance");
        }
        this.settlement = this.settlement.minus(amount);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
