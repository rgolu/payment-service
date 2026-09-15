package com.payments.fee;

import com.payments.domain.money.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeeScheduleTest {

    @Test
    void firstTierOnly() {
        // ₹1,000 @ 2% = ₹20
        assertThat(DefaultFeeSchedules.UPI.compute(Money.rupees("1000")))
                .isEqualTo(Money.rupees("20"));
    }

    @Test
    void spansAllThreeTiers() {
        // ₹6,000 = 2000*2% + 3000*1.5% + 1000*1% = 40 + 45 + 10 = ₹95
        assertThat(DefaultFeeSchedules.UPI.compute(Money.rupees("6000")))
                .isEqualTo(Money.rupees("95"));
    }

    @Test
    void exactTierBoundaryUsesTheLowerBand() {
        // ₹2,000 is still the 2% band
        assertThat(DefaultFeeSchedules.UPI.compute(Money.rupees("2000")))
                .isEqualTo(Money.rupees("40"));
        // ₹2,001 = 2000*2% + 1*1.5% = 40 + 0.015 → ₹40.02
        assertThat(DefaultFeeSchedules.UPI.compute(Money.rupees("2001")))
                .isEqualTo(Money.rupees("40.02"));
        // ₹5,000 = 2000*2% + 3000*1.5% = 40 + 45 = ₹85
        assertThat(DefaultFeeSchedules.UPI.compute(Money.rupees("5000")))
                .isEqualTo(Money.rupees("85"));
    }

    @Test
    void minimumFeeFloorsSmallPayments() {
        // ₹100 @ 2% = ₹2, floored to ₹5
        assertThat(DefaultFeeSchedules.UPI.compute(Money.rupees("100")))
                .isEqualTo(Money.rupees("5"));
        // ₹250 @ 2% = ₹5 exactly — min fee does not inflate further
        assertThat(DefaultFeeSchedules.UPI.compute(Money.rupees("250")))
                .isEqualTo(Money.rupees("5"));
    }

    @Test
    void zeroPrincipalChargesNothing() {
        assertThat(DefaultFeeSchedules.UPI.compute(Money.ZERO)).isEqualTo(Money.ZERO);
    }

    @Test
    void roundsHalfUpToNearestPaisa() {
        // ₹99.99 * 2% = 1.9998 → ₹2.00, then min fee → ₹5
        assertThat(DefaultFeeSchedules.UPI.compute(Money.rupees("99.99")))
                .isEqualTo(Money.rupees("5"));
        // ₹1,333.33 * 2% = 26.6666 → ₹26.67 (above min)
        assertThat(DefaultFeeSchedules.UPI.compute(Money.rupees("1333.33")))
                .isEqualTo(Money.rupees("26.67"));
    }
}
