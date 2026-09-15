package com.payments.coupon;

import com.payments.domain.enums.CouponType;
import com.payments.domain.exception.CouponException;
import com.payments.domain.model.Coupon;
import com.payments.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponEngineTest {

    private final CouponEngine engine = new CouponEngine();
    private final Instant now = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    void percentDiscount() {
        Coupon coupon = new Coupon("SAVE10", CouponType.PERCENT, 10, Money.ZERO, null, null, null);
        assertThat(engine.discount(coupon, Money.rupees("1000"), now)).isEqualTo(Money.rupees("100"));
    }

    @Test
    void percentHonoursMaxCap() {
        Coupon coupon = new Coupon("SAVE10", CouponType.PERCENT, 10, Money.ZERO, Money.rupees("50"), null, null);
        assertThat(engine.discount(coupon, Money.rupees("1000"), now)).isEqualTo(Money.rupees("50"));
    }

    @Test
    void flatDiscount() {
        Coupon coupon = new Coupon("FLAT100", CouponType.FLAT, 10_000, Money.ZERO, null, null, null);
        assertThat(engine.discount(coupon, Money.rupees("1000"), now)).isEqualTo(Money.rupees("100"));
    }

    @Test
    void flatCannotExceedPrincipal() {
        Coupon coupon = new Coupon("FLAT999", CouponType.FLAT, 50_000, Money.ZERO, null, null, null);
        assertThat(engine.discount(coupon, Money.rupees("100"), now)).isEqualTo(Money.rupees("100"));
    }

    @Test
    void rejectsDeletedExpiredExhaustedAndBelowMin() {
        Coupon deleted = new Coupon("X", CouponType.PERCENT, 10, Money.ZERO, null, null, null);
        deleted.deactivate();
        assertThatThrownBy(() -> engine.discount(deleted, Money.rupees("100"), now))
                .isInstanceOf(CouponException.class);

        Coupon expired = new Coupon("E", CouponType.PERCENT, 10, Money.ZERO, null, null, now.minusSeconds(1));
        assertThatThrownBy(() -> engine.discount(expired, Money.rupees("100"), now))
                .isInstanceOf(CouponException.class);

        Coupon exhausted = new Coupon("Z", CouponType.PERCENT, 10, Money.ZERO, null, 0, null);
        assertThatThrownBy(() -> engine.discount(exhausted, Money.rupees("100"), now))
                .isInstanceOf(CouponException.class);

        Coupon min = new Coupon("M", CouponType.PERCENT, 10, Money.rupees("500"), null, null, null);
        assertThatThrownBy(() -> engine.discount(min, Money.rupees("100"), now))
                .isInstanceOf(CouponException.class);
    }
}
