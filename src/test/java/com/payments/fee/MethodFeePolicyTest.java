package com.payments.fee;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MethodFeePolicyTest {

    @Test
    void cardIsMoreExpensiveThanUpiOnSameAmount() {
        MethodFeePolicy policy = new MethodFeePolicy(DefaultFeeSchedules.all(), new IdentityFeeMultiplier());
        Money amount = Money.rupees("1000");
        Money upi = policy.quoteFee(amount, PaymentMethod.UPI);
        Money card = policy.quoteFee(amount, PaymentMethod.CARD);
        assertThat(upi).isEqualTo(Money.rupees("20"));
        assertThat(card).isEqualTo(Money.rupees("25"));
        assertThat(card).isGreaterThan(upi);
    }

    @Test
    void cardMinimumFeeIsHigher() {
        MethodFeePolicy policy = new MethodFeePolicy(DefaultFeeSchedules.all(), new IdentityFeeMultiplier());
        assertThat(policy.quoteFee(Money.rupees("100"), PaymentMethod.UPI)).isEqualTo(Money.rupees("5"));
        assertThat(policy.quoteFee(Money.rupees("100"), PaymentMethod.CARD)).isEqualTo(Money.rupees("8"));
    }

    @Test
    void loadMultiplierAppliesAfterTieredFee() {
        LoadFeeMultiplier load = new LoadFeeMultiplier();
        load.setLoad(PaymentMethod.UPI, new BigDecimal("1.20"));
        MethodFeePolicy policy = new MethodFeePolicy(DefaultFeeSchedules.all(), load);
        // ₹1,000 UPI fee ₹20 * 1.2 = ₹24
        assertThat(policy.quoteFee(Money.rupees("1000"), PaymentMethod.UPI))
                .isEqualTo(Money.rupees("24"));
        // Card untouched
        assertThat(policy.quoteFee(Money.rupees("1000"), PaymentMethod.CARD))
                .isEqualTo(Money.rupees("25"));
    }

    @Test
    void quoteFee_UnknownMethod_Throws() {
        MethodFeePolicy policy = new MethodFeePolicy(new EnumMap<>(PaymentMethod.class), new IdentityFeeMultiplier());
        policy.register(PaymentMethod.UPI, DefaultFeeSchedules.UPI);
        assertThatThrownBy(() -> policy.quoteFee(Money.rupees("100"), PaymentMethod.CARD))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
