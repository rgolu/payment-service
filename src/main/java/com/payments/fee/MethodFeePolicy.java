package com.payments.fee;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.money.Money;

import java.util.EnumMap;
import java.util.Map;

/**
 * Looks up a method's schedule, then applies an optional load multiplier.
 */
public class MethodFeePolicy implements FeePolicy {

    private final Map<PaymentMethod, FeeSchedule> schedules;
    private final FeeMultiplier multiplier;

    public MethodFeePolicy(Map<PaymentMethod, FeeSchedule> schedules, FeeMultiplier multiplier) {
        this.schedules = new EnumMap<>(schedules);
        this.multiplier = multiplier;
    }

    public void register(PaymentMethod method, FeeSchedule schedule) {
        schedules.put(method, schedule);
    }

    @Override
    public Money quoteFee(Money amount, PaymentMethod feeMethod) {
        FeeSchedule schedule = schedules.get(feeMethod);
        if (schedule == null) {
            throw new IllegalArgumentException("no fee schedule for " + feeMethod);
        }
        Money base = schedule.compute(amount);
        return base.times(multiplier.factor(feeMethod));
    }
}
