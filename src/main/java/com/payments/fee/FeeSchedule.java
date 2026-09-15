package com.payments.fee;

import com.payments.domain.money.Money;

import java.util.List;

/**
 * Tiered schedule with a floor. Append a {@link FeeTier}
 * (ordered by {@code upTo}; last tier {@code upTo == null}).
 */
public record FeeSchedule(Money minFee, List<FeeTier> tiers) {

    public Money compute(Money amount) {
        if (amount.isZero()) {
            // A 100% coupon should not still levy the platform minimum.
            return Money.ZERO;
        }

        Money fee = Money.ZERO;
        long prevCap = 0;
        long remaining = amount.paise();

        for (FeeTier tier : tiers) {
            Long cap = tier.upTo() == null ? null : tier.upTo().paise();
            long bandWidth = cap == null ? remaining : Math.min(remaining, cap - prevCap);
            if (bandWidth > 0) {
                fee = fee.plus(Money.ofPaise(bandWidth).mulBps(tier.rateBps()));
                remaining -= bandWidth;
            }
            if (remaining <= 0) {
                break;
            }
            if (cap != null) {
                prevCap = cap;
            }
        }

        return fee.compareTo(minFee) >= 0 ? fee : minFee;
    }
}
