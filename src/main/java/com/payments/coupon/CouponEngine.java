package com.payments.coupon;

import com.payments.domain.enums.CouponType;
import com.payments.domain.exception.CouponException;
import com.payments.domain.model.Coupon;
import com.payments.domain.money.Money;

import java.time.Instant;

public class CouponEngine {

    public Money discount(Coupon coupon, Money amount, Instant now) {
        ensureApplicable(coupon, amount, now);
        Money raw;
        if (coupon.getType() == CouponType.PERCENT) {
            raw = amount.percent(coupon.getValue());
        } else if (coupon.getType() == CouponType.FLAT) {
            raw = Money.ofPaise(Math.min(coupon.getValue(), amount.paise()));
        } else {
            throw new CouponException("unsupported coupon type: " + coupon.getType());
        }
        if (coupon.getMaxDiscount() != null && raw.compareTo(coupon.getMaxDiscount()) > 0) {
            raw = coupon.getMaxDiscount();
        }
        return raw;
    }

    public void ensureApplicable(Coupon coupon, Money amount, Instant now) {
        if (!coupon.isActive()) {
            throw new CouponException("coupon " + coupon.getCode() + " has been deleted");
        }
        if (coupon.getExpiresAt() != null && !now.isBefore(coupon.getExpiresAt())) {
            throw new CouponException("coupon " + coupon.getCode() + " has expired");
        }
        if (coupon.getRemainingUses() != null && coupon.getRemainingUses() <= 0) {
            throw new CouponException("coupon " + coupon.getCode() + " has no remaining uses");
        }
        if (amount.compareTo(coupon.getMinAmount()) < 0) {
            throw new CouponException(
                    "coupon " + coupon.getCode() + " requires min amount " + coupon.getMinAmount()
            );
        }
        if (coupon.getType() == CouponType.PERCENT && (coupon.getValue() <= 0 || coupon.getValue() > 100)) {
            throw new CouponException("percent coupon value must be in (0, 100]");
        }
        if (coupon.getType() == CouponType.FLAT && coupon.getValue() <= 0) {
            throw new CouponException("flat coupon value must be positive");
        }
    }
}
