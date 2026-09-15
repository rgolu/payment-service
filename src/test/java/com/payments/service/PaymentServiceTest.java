package com.payments.service;

import com.payments.TestHarness;
import com.payments.domain.enums.CouponType;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.PaymentStatus;
import com.payments.domain.exception.CouponException;
import com.payments.domain.exception.InsufficientBalanceException;
import com.payments.domain.exception.InvalidStateException;
import com.payments.domain.exception.PaymentException;
import com.payments.domain.model.Merchant;
import com.payments.domain.model.Payment;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private TestHarness h;
    private User user;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        h = new TestHarness();
        user = h.users.register("Rishav", Money.rupees("10000"));
        merchant = h.merchants.register("Cafe", Set.of(PaymentMethod.UPI, PaymentMethod.CARD));
    }

    @Test
    void initiateDebitsWalletAndCompleteCreditsMerchant() {
        Payment pending = h.payments.initiate(
                user.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, null
        );
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(pending.getQuote().fee()).isEqualTo(Money.rupees("20"));
        assertThat(pending.amountCharged()).isEqualTo(Money.rupees("1020"));
        assertThat(user.getWallet()).isEqualTo(Money.rupees("8980"));
        assertThat(merchant.getSettlement()).isEqualTo(Money.ZERO);

        Payment done = h.payments.complete(pending.getId());
        assertThat(done.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(done.amountCharged()).isEqualTo(Money.rupees("1020"));
        assertThat(merchant.getSettlement()).isEqualTo(Money.rupees("1000"));
    }

    @Test
    void insufficientBalanceIsRejectedWithoutSideEffects() {
        User poor = h.users.register("Poor", Money.rupees("10"));
        assertThatThrownBy(() -> h.payments.initiate(
                poor.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, null
        )).isInstanceOf(InsufficientBalanceException.class);
        assertThat(poor.getWallet()).isEqualTo(Money.rupees("10"));
        assertThat(h.payments.historyForUser(poor.getId())).isEmpty();
    }

    @Test
    void couponReducesPrincipalThenFeeIsQuotedOnDiscountedAmount() {
        h.coupons.add("SAVE10", CouponType.PERCENT, 10, Money.ZERO, null, 1, null);
        Payment payment = h.payments.initiate(
                user.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, "SAVE10"
        );
        // principal 900, UPI fee 18, total 918
        assertThat(payment.getQuote().discount()).isEqualTo(Money.rupees("100"));
        assertThat(payment.getQuote().principal()).isEqualTo(Money.rupees("900"));
        assertThat(payment.getQuote().fee()).isEqualTo(Money.rupees("18"));
        assertThat(payment.amountCharged()).isEqualTo(Money.rupees("918"));
        assertThat(user.getWallet()).isEqualTo(Money.rupees("9082"));
    }

    @Test
    void invalidCouponDoesNotDebit() {
        Money before = user.getWallet();
        assertThatThrownBy(() -> h.payments.initiate(
                user.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, "NOPE"
        )).isInstanceOf(CouponException.class);
        assertThat(user.getWallet()).isEqualTo(before);
    }

    @Test
    void deletedCouponIsRejected() {
        h.coupons.add("GONE", CouponType.FLAT, 1000, Money.ZERO, null, null, null);
        h.coupons.delete("GONE");
        assertThatThrownBy(() -> h.payments.initiate(
                user.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, "GONE"
        )).isInstanceOf(CouponException.class);
    }

    @Test
    void upiDowntimeReroutesToCardAtUpiFee() {
        h.providers.setAvailable(PaymentMethod.UPI, false);
        Payment payment = h.payments.initiate(
                user.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, null
        );
        assertThat(payment.getQuote().rerouted()).isTrue();
        assertThat(payment.getQuote().executedMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(payment.getQuote().feeMethod()).isEqualTo(PaymentMethod.UPI);
        assertThat(payment.getQuote().fee()).isEqualTo(Money.rupees("20"));
        // Card schedule would have been ₹25 — user must not pay the delta
        assertThat(payment.getQuote().fee()).isLessThan(Money.rupees("25"));
    }

    @Test
    void merchantMustSupportRequestedMethod() {
        Merchant cards = h.merchants.register("POS", Set.of(PaymentMethod.CARD));
        assertThatThrownBy(() -> h.payments.initiate(
                user.getId(), cards.getId(), Money.rupees("100"), PaymentMethod.UPI, null
        )).isInstanceOf(PaymentException.class);
    }

    @Test
    void historyIncludesPendingAndCompleted() {
        Payment a = h.payments.initiate(
                user.getId(), merchant.getId(), Money.rupees("100"), PaymentMethod.UPI, null
        );
        Payment b = h.payments.initiate(
                user.getId(), merchant.getId(), Money.rupees("200"), PaymentMethod.CARD, null
        );
        h.payments.complete(a.getId());

        assertThat(h.payments.historyForUser(user.getId()))
                .extracting(Payment::getStatus)
                .containsExactlyInAnyOrder(PaymentStatus.COMPLETED, PaymentStatus.PENDING);
        assertThat(h.payments.historyForMerchant(merchant.getId())).hasSize(2);
        assertThat(h.payments.get(b.getId()).getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void cannotCompleteTwice() {
        Payment payment = h.payments.initiate(
                user.getId(), merchant.getId(), Money.rupees("100"), PaymentMethod.UPI, null
        );
        h.payments.complete(payment.getId());
        assertThatThrownBy(() -> h.payments.complete(payment.getId()))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void refundReturnsPrincipalMinusRefundFee() {
        Payment payment = h.payments.initiate(
                user.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, null
        );
        h.payments.complete(payment.getId());
        Money walletAfterPay = user.getWallet();

        Payment refunded = h.payments.refund(payment.getId());
        // refund fee = max(₹2, 1% of 1000) = ₹10; user gets ₹990
        assertThat(refunded.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(refunded.getRefundFee()).isEqualTo(Money.rupees("10"));
        assertThat(refunded.getRefundedToUser()).isEqualTo(Money.rupees("990"));
        assertThat(user.getWallet()).isEqualTo(walletAfterPay.plus(Money.rupees("990")));
        assertThat(merchant.getSettlement()).isEqualTo(Money.ZERO);
    }

    @Test
    void topUpIncreasesSpendableBalance() {
        h.users.topUp(user.getId(), Money.rupees("50"));
        assertThat(user.getWallet()).isEqualTo(Money.rupees("10050"));
    }
}
