package com.payments.service;

import org.springframework.scheduling.annotation.Scheduled;

public class PendingPaymentSweeper {

    private final PaymentService payments;

    public PendingPaymentSweeper(PaymentService payments) {
        this.payments = payments;
    }

    @Scheduled(fixedDelayString = "${payments.expiry-sweep-ms:30000}")
    public void sweep() {
        payments.expireStale();
    }
}
