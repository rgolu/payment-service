package com.payments.service;

import com.payments.TestHarness;
import com.payments.domain.enums.CouponType;
import com.payments.domain.exception.CouponException;
import com.payments.domain.exception.NotFoundException;
import com.payments.domain.model.Coupon;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.payments.common.TestConstants.COUPON_SAVE10;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CouponServiceTest {

    private TestHarness h;

    @BeforeEach
    void setUp() {
        h = new TestHarness();
    }

    @Test
    void add_PercentCoupon_NormalizesCode() {
        Coupon coupon = h.coupons.add("save10", CouponType.PERCENT, 10, Money.ZERO, null, 2, null);
        assertEquals(COUPON_SAVE10, coupon.getCode());
        assertEquals(10, coupon.getValue());
    }

    @Test
    void add_BlankCode_Throws() {
        assertThrows(CouponException.class, () -> h.coupons.add("   ", CouponType.PERCENT, 10, Money.ZERO, null, null, null));
    }

    @Test
    void add_DuplicateActive_Throws() {
        h.coupons.add(COUPON_SAVE10, CouponType.PERCENT, 10, Money.ZERO, null, null, null);
        assertThrows(CouponException.class, () ->
                h.coupons.add(COUPON_SAVE10, CouponType.PERCENT, 10, Money.ZERO, null, null, null));
    }

    @Test
    void delete_Existing_Deactivates() {
        h.coupons.add(COUPON_SAVE10, CouponType.PERCENT, 10, Money.ZERO, null, null, null);
        h.coupons.delete(COUPON_SAVE10);
        assertThrows(CouponException.class, () -> h.coupons.requireActive(COUPON_SAVE10));
    }

    @Test
    void delete_Unknown_Throws() {
        assertThrows(NotFoundException.class, () -> h.coupons.delete("NOPE"));
    }

    @Test
    void requireActive_Unknown_Throws() {
        assertThrows(CouponException.class, () -> h.coupons.requireActive("NOPE"));
    }
}
