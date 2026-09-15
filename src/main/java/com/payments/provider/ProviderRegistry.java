package com.payments.provider;

import com.payments.domain.enums.PaymentMethod;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProviderRegistry {

    private final Map<PaymentMethod, Boolean> up = new ConcurrentHashMap<>();
    private final Map<PaymentMethod, Double> successRate = new ConcurrentHashMap<>();

    public ProviderRegistry() {
        up.put(PaymentMethod.UPI, true);
        up.put(PaymentMethod.CARD, true);
        successRate.put(PaymentMethod.UPI, 0.97);
        successRate.put(PaymentMethod.CARD, 0.93);
    }

    public boolean isAvailable(PaymentMethod method) {
        return up.getOrDefault(method, false);
    }

    public void setAvailable(PaymentMethod method, boolean available) {
        up.put(method, available);
    }

    public double successRate(PaymentMethod method) {
        return successRate.getOrDefault(method, 0.0);
    }

    public void setSuccessRate(PaymentMethod method, double rate) {
        if (rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("success rate must be in [0, 1]");
        }
        successRate.put(method, rate);
    }

    public void register(PaymentMethod method, boolean available, double successRateValue) {
        up.put(method, available);
        successRate.put(method, successRateValue);
    }

    public Map<PaymentMethod, Boolean> availabilitySnapshot() {
        return new EnumMap<>(up);
    }
}
