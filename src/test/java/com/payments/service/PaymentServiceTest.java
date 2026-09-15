package com.payments.service;

import com.payments.TestHarness;
import com.payments.domain.enums.CouponType;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.PaymentStatus;
import com.payments.domain.exception.CouponException;
import com.payments.domain.exception.DuplicateEntityException;
import com.payments.domain.exception.InsufficientBalanceException;
import com.payments.domain.exception.InvalidStateException;
import com.payments.domain.exception.NotFoundException;
import com.payments.domain.exception.PaymentException;
import com.payments.domain.model.Merchant;
import com.payments.domain.model.Payment;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static com.payments.common.TestConstants.COUPON_SAVE10;
import static com.payments.common.TestConstants.MERCHANT_NAME;
import static com.payments.common.TestConstants.PAYMENT_ID;
import static com.payments.common.TestConstants.POOR_USER_NAME;
import static com.payments.common.TestConstants.REFUND_ID;
import static com.payments.common.TestConstants.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PaymentServiceTest {

    private TestHarness h;
    private User user;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        h = new TestHarness();
        user = h.users.register(USER_NAME, Money.rupees("10000"));
        merchant = h.merchants.register(MERCHANT_NAME, Set.of(PaymentMethod.UPI, PaymentMethod.CARD));
    }

    private Payment initiate(Money amount, PaymentMethod method, String coupon) {
        return h.payments.initiate(h.nextPaymentId(), user.getId(), merchant.getId(), amount, method, coupon);
    }

    @Test
    void initiate_UpiThousand_DebitsWalletWithUpiFee() {
        // GIVEN
        Money amount = Money.rupees("1000");

        // TEST
        Payment pending = initiate(amount, PaymentMethod.UPI, null);

        // VERIFY
        assertEquals(PaymentStatus.PENDING, pending.getStatus());
        assertEquals(Money.rupees("20"), pending.getQuote().fee());
        assertEquals(Money.rupees("1020"), pending.amountCharged());
        assertEquals(PaymentMethod.UPI, pending.getQuote().executedMethod());
        assertEquals(PaymentMethod.UPI, pending.getQuote().feeMethod());
        assertEquals(Money.rupees("8980"), user.getWallet());
        assertEquals(Money.ZERO, merchant.getSettlement());
    }

    @Test
    void complete_PendingPayment_CreditsMerchantPrincipal() {
        // GIVEN
        Payment pending = initiate(Money.rupees("1000"), PaymentMethod.UPI, null);

        // TEST
        Payment done = h.payments.complete(pending.getId());

        // VERIFY
        assertEquals(PaymentStatus.COMPLETED, done.getStatus());
        assertEquals(Money.rupees("1020"), done.amountCharged());
        assertEquals(Money.rupees("1000"), merchant.getSettlement());
    }

    @Test
    void initiate_InsufficientBalance_ThrowsWithoutDebit() {
        // GIVEN
        User poor = h.users.register(POOR_USER_NAME, Money.rupees("10"));

        // TEST
        assertThrows(InsufficientBalanceException.class, () -> h.payments.initiate(
                h.nextPaymentId(), poor.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, null
        ));

        // VERIFY
        assertEquals(Money.rupees("10"), poor.getWallet());
        assertTrue(h.payments.historyForUser(poor.getId()).isEmpty());
    }

    @Test
    void initiate_CardThousand_ChargesHigherCardFee() {
        // GIVEN
        Money amount = Money.rupees("1000");

        // TEST
        Payment payment = initiate(amount, PaymentMethod.CARD, null);

        // VERIFY
        assertEquals(Money.rupees("25"), payment.getQuote().fee());
        assertEquals(Money.rupees("1025"), payment.amountCharged());
        assertEquals(PaymentMethod.CARD, payment.getQuote().feeMethod());
        assertEquals(Money.rupees("8975"), user.getWallet());
    }

    @Test
    void initiate_UpiProviderDown_ReroutesToCardAtUpiFee() {
        // GIVEN
        h.providers.setAvailable(PaymentMethod.UPI, false);

        // TEST
        Payment payment = initiate(Money.rupees("1000"), PaymentMethod.UPI, null);

        // VERIFY
        assertTrue(payment.getQuote().rerouted());
        assertEquals(PaymentMethod.CARD, payment.getQuote().executedMethod());
        assertEquals(PaymentMethod.UPI, payment.getQuote().feeMethod());
        assertEquals(Money.rupees("20"), payment.getQuote().fee());
        assertEquals(Money.rupees("1020"), payment.amountCharged());
        assertEquals(Money.rupees("8980"), user.getWallet());
    }

    @Test
    void initiate_PercentCoupon_FeesQuotedOnDiscountedPrincipal() {
        // GIVEN
        h.coupons.add(COUPON_SAVE10, CouponType.PERCENT, 10, Money.ZERO, null, 1, null);

        // TEST
        Payment payment = initiate(Money.rupees("1000"), PaymentMethod.UPI, COUPON_SAVE10);

        // VERIFY
        assertEquals(Money.rupees("100"), payment.getQuote().discount());
        assertEquals(Money.rupees("900"), payment.getQuote().principal());
        assertEquals(Money.rupees("18"), payment.getQuote().fee());
        assertEquals(Money.rupees("918"), payment.amountCharged());
        assertEquals(Money.rupees("9082"), user.getWallet());
    }

    @Test
    void initiate_UnknownCoupon_DoesNotDebit() {
        // GIVEN
        Money before = user.getWallet();

        // TEST
        assertThrows(CouponException.class, () -> initiate(Money.rupees("1000"), PaymentMethod.UPI, "NOPE"));

        // VERIFY
        assertEquals(before, user.getWallet());
    }

    @Test
    void initiate_DeletedCoupon_Throws() {
        // GIVEN
        h.coupons.add("GONE", CouponType.FLAT, 1000, Money.ZERO, null, null, null);
        h.coupons.delete("GONE");

        // TEST + VERIFY
        assertThrows(CouponException.class, () -> initiate(Money.rupees("1000"), PaymentMethod.UPI, "GONE"));
    }

    @Test
    void initiate_MerchantLacksMethod_Throws() {
        // GIVEN
        Merchant cards = h.merchants.register("POS", Set.of(PaymentMethod.CARD));

        // TEST + VERIFY
        assertThrows(PaymentException.class, () -> h.payments.initiate(
                h.nextPaymentId(), user.getId(), cards.getId(), Money.rupees("100"), PaymentMethod.UPI, null
        ));
    }

    @Test
    void initiate_ZeroAmount_Throws() {
        assertThrows(InvalidStateException.class, () -> initiate(Money.ZERO, PaymentMethod.UPI, null));
    }

    @Test
    void history_PendingAndCompleted_ReturnedForUserAndMerchant() {
        // GIVEN
        Payment a = initiate(Money.rupees("100"), PaymentMethod.UPI, null);
        Payment b = initiate(Money.rupees("200"), PaymentMethod.CARD, null);
        h.payments.complete(a.getId());

        // TEST
        var userHistory = h.payments.historyForUser(user.getId());
        var merchantHistory = h.payments.historyForMerchant(merchant.getId());

        // VERIFY
        assertEquals(2, userHistory.size());
        assertEquals(2, merchantHistory.size());
        assertEquals(PaymentStatus.PENDING, h.payments.get(b.getId()).getStatus());
    }

    @Test
    void complete_AlreadyCompleted_IsIdempotent() {
        // GIVEN
        Payment payment = initiate(Money.rupees("100"), PaymentMethod.UPI, null);
        Payment first = h.payments.complete(payment.getId());

        // TEST
        Payment second = h.payments.complete(payment.getId());

        // VERIFY
        assertEquals(PaymentStatus.COMPLETED, second.getStatus());
        assertEquals(first.getCompletedAt(), second.getCompletedAt());
        assertEquals(Money.rupees("100"), merchant.getSettlement());
    }

    @Test
    void refund_CompletedPayment_ReturnsPrincipalMinusRefundFee() {
        // GIVEN
        Payment payment = initiate(Money.rupees("1000"), PaymentMethod.UPI, null);
        h.payments.complete(payment.getId());
        Money walletAfterPay = user.getWallet();

        // TEST
        Payment refunded = h.payments.refund(payment.getId(), REFUND_ID);

        // VERIFY
        assertEquals(PaymentStatus.REFUNDED, refunded.getStatus());
        assertEquals(Money.rupees("10"), refunded.getRefundFee());
        assertEquals(Money.rupees("990"), refunded.getRefundedToUser());
        assertEquals(walletAfterPay.plus(Money.rupees("990")), user.getWallet());
        assertEquals(Money.ZERO, merchant.getSettlement());
    }

    @Test
    void refund_PendingPayment_Throws() {
        // GIVEN
        Payment pending = initiate(Money.rupees("100"), PaymentMethod.UPI, null);

        // TEST + VERIFY
        assertThrows(InvalidStateException.class, () -> h.payments.refund(pending.getId(), REFUND_ID));
    }

    @Test
    void refund_ReplaySameRefundId_IsIdempotent() {
        // GIVEN
        Payment payment = initiate(Money.rupees("1000"), PaymentMethod.UPI, null);
        h.payments.complete(payment.getId());
        Payment first = h.payments.refund(payment.getId(), "ref_same");

        // TEST
        Payment replay = h.payments.refund(payment.getId(), "ref_same");

        // VERIFY
        assertEquals(first.getRefundedAt(), replay.getRefundedAt());
        assertThrows(DuplicateEntityException.class, () -> h.payments.refund(payment.getId(), "ref_other"));
    }

    @Test
    void topUp_CreditId_IncreasesSpendableBalance() {
        // TEST
        h.users.topUp(user.getId(), Money.rupees("50"), "crd_1");

        // VERIFY
        assertEquals(Money.rupees("10050"), user.getWallet());
    }

    @Test
    void initiate_SamePaymentId_ReplaysWithoutDoubleDebit() {
        // GIVEN
        Payment first = h.payments.initiate(
                PAYMENT_ID, user.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, null
        );
        Money afterFirst = user.getWallet();

        // TEST
        Payment replay = h.payments.initiate(
                PAYMENT_ID, user.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, null
        );

        // VERIFY
        assertEquals(first.getId(), replay.getId());
        assertEquals(afterFirst, user.getWallet());
    }

    @Test
    void initiate_SamePaymentIdDifferentPayload_Conflicts() {
        // GIVEN
        h.payments.initiate(
                "pay_conflict", user.getId(), merchant.getId(), Money.rupees("1000"), PaymentMethod.UPI, null
        );

        // TEST + VERIFY
        assertThrows(DuplicateEntityException.class, () -> h.payments.initiate(
                "pay_conflict", user.getId(), merchant.getId(), Money.rupees("500"), PaymentMethod.UPI, null
        ));
    }

    @Test
    void get_PendingPastTtl_ExpiresAndReleasesWalletHold() {
        // GIVEN
        Payment pending = initiate(Money.rupees("1000"), PaymentMethod.UPI, null);
        assertEquals(Money.rupees("8980"), user.getWallet());
        h.clock.advance(Duration.ofMinutes(15));

        // TEST
        Payment expired = h.payments.get(pending.getId());

        // VERIFY
        assertEquals(PaymentStatus.EXPIRED, expired.getStatus());
        assertEquals(Money.rupees("10000"), user.getWallet());
        assertThrows(InvalidStateException.class, () -> h.payments.complete(pending.getId()));
    }

    @Test
    void expireStale_PendingPastTtl_ExpiresAndCounts() {
        // GIVEN
        initiate(Money.rupees("100"), PaymentMethod.UPI, null);
        h.clock.advance(Duration.ofMinutes(15));

        // TEST
        int expired = h.payments.expireStale();

        // VERIFY
        assertEquals(1, expired);
        assertEquals(Money.rupees("10000"), user.getWallet());
    }

    @Test
    void get_UnknownPayment_ThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> h.payments.get("pay_missing"));
    }
}
