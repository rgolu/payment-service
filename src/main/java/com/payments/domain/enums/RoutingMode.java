package com.payments.domain.enums;

/** Switchable without touching payment orchestration. */
public enum RoutingMode {
    FAILOVER,
    CHEAPEST,
    SUCCESS_RATE
}
