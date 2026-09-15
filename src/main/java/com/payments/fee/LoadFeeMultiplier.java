package com.payments.fee;

import com.payments.domain.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bonus: pluggable peak/load surcharge per provider.
 * Applied on the already-computed fee (including min fee).
 */
public class LoadFeeMultiplier implements FeeMultiplier {

    private final Map<PaymentMethod, BigDecimal> load = new ConcurrentHashMap<>();

    public void setLoad(PaymentMethod method, BigDecimal factor) {
        if (factor.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("load factor must be >= 1.0");
        }
        load.put(method, factor);
    }

    @Override
    public BigDecimal factor(PaymentMethod method) {
        return load.getOrDefault(method, BigDecimal.ONE);
    }

    public Map<PaymentMethod, BigDecimal> snapshot() {
        return new EnumMap<>(load);
    }
}
