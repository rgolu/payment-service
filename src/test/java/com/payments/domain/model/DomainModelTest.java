package com.payments.domain.model;

import com.payments.domain.enums.CouponType;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.PaymentStatus;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DomainModelTest {

    @Test
    void user_DebitInsufficient_Throws() {
        User user = new User("usr_1", "Rishav", Money.rupees("10"));
        assertThrows(IllegalStateException.class, () -> user.debit(Money.rupees("11")));
        user.debit(Money.rupees("10"));
        assertEquals(Money.ZERO, user.getWallet());
    }

    @Test
    void merchant_DebitSettlementInsufficient_Throws() {
        Merchant merchant = new Merchant("mer_1", "Cafe", Set.of(PaymentMethod.UPI));
        assertThrows(IllegalStateException.class, () -> merchant.debitSettlement(Money.rupees("1")));
        merchant.creditSettlement(Money.rupees("5"));
        merchant.debitSettlement(Money.rupees("5"));
        assertEquals(Money.ZERO, merchant.getSettlement());
        assertEquals("Cafe", merchant.getName());
        assertTrue(merchant.getCreatedAt().isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void coupon_ConsumeAndDeactivate() {
        Coupon coupon = new Coupon("save10", CouponType.PERCENT, 10, null, Money.rupees("50"), 2, Instant.now());
        assertEquals("SAVE10", coupon.getCode());
        assertEquals(CouponType.PERCENT, coupon.getType());
        assertEquals(Money.ZERO, coupon.getMinAmount());
        assertEquals(Money.rupees("50"), coupon.getMaxDiscount());
        coupon.consumeUse();
        assertEquals(1, coupon.getRemainingUses());
        coupon.deactivate();
        assertFalse(coupon.isActive());
        Coupon unlimited = new Coupon("FLAT", CouponType.FLAT, 100, Money.ZERO, null, null, null);
        unlimited.consumeUse();
        assertEquals(null, unlimited.getRemainingUses());
    }

    @Test
    void payment_MarkFailedExpiredAndSameRequest() {
        Instant now = Instant.parse("2026-09-15T00:00:00Z");
        Quote quote = new Quote(
                Money.rupees("100"), Money.ZERO, Money.rupees("100"), Money.rupees("5"),
                Money.rupees("105"), PaymentMethod.UPI, PaymentMethod.UPI, PaymentMethod.UPI, false, null
        );
        Payment payment = new Payment("pay_1", "usr_1", "mer_1", quote, now, now.plusSeconds(60));
        assertEquals(Money.rupees("105"), payment.amountCharged());
        assertTrue(payment.sameInitiateRequest("usr_1", "mer_1", Money.rupees("100"), PaymentMethod.UPI, "  "));
        assertFalse(payment.sameInitiateRequest("usr_1", "mer_1", Money.rupees("100"), PaymentMethod.UPI, "SAVE10"));
        payment.markFailed("down");
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals("down", payment.getFailureReason());
        assertFalse(payment.isExpired(now.plusSeconds(120)));
    }
}
