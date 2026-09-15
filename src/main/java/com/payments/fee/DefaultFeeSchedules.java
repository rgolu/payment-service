package com.payments.fee;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.money.Money;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Spec example is treated as the UPI schedule. Card is deliberately more expensive
 * so "no extra fee" on UPI→Card reroute has a visible delta.
 */
public final class DefaultFeeSchedules {

    private DefaultFeeSchedules() {
    }

    public static final FeeSchedule UPI = new FeeSchedule(
            Money.rupees("5"),
            List.of(
                    new FeeTier(Money.rupees("2000"), 200),
                    new FeeTier(Money.rupees("5000"), 150),
                    new FeeTier(null, 100)
            )
    );

    public static final FeeSchedule CARD = new FeeSchedule(
            Money.rupees("8"),
            List.of(
                    new FeeTier(Money.rupees("2000"), 250),
                    new FeeTier(Money.rupees("5000"), 200),
                    new FeeTier(null, 150)
            )
    );

    public static Map<PaymentMethod, FeeSchedule> all() {
        EnumMap<PaymentMethod, FeeSchedule> map = new EnumMap<>(PaymentMethod.class);
        map.put(PaymentMethod.UPI, UPI);
        map.put(PaymentMethod.CARD, CARD);
        return map;
    }
}
