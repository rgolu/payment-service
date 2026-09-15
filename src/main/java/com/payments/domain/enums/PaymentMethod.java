package com.payments.domain.enums;

/**
 * Seam for live extension: add a constant, then register a {@code FeeSchedule}
 * and a provider in {@code AppConfig} / {@code ProviderRegistry}.
 */
public enum PaymentMethod {
    UPI,
    CARD
    // NETBANKING  ← live-extension hook
}
