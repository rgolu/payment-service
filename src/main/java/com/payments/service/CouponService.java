package com.payments.service;

import com.payments.domain.enums.CouponType;
import com.payments.domain.exception.CouponException;
import com.payments.domain.exception.NotFoundException;
import com.payments.domain.model.Coupon;
import com.payments.domain.money.Money;
import com.payments.repository.CouponRepository;

import java.time.Instant;

public class CouponService {

    private final CouponRepository coupons;

    public CouponService(CouponRepository coupons) {
        this.coupons = coupons;
    }

    public Coupon add(
            String code,
            CouponType type,
            int value,
            Money minAmount,
            Money maxDiscount,
            Integer remainingUses,
            Instant expiresAt
    ) {
        String normalized = code.trim().toUpperCase();
        if (normalized.isEmpty()) {
            throw new CouponException("coupon code is required");
        }
        if (coupons.findByCode(normalized).filter(Coupon::isActive).isPresent()) {
            throw new CouponException("coupon already exists: " + normalized);
        }
        Coupon coupon = new Coupon(normalized, type, value, minAmount, maxDiscount, remainingUses, expiresAt);
        return coupons.save(coupon);
    }

    public void delete(String code) {
        Coupon coupon = coupons.findByCode(code)
                .orElseThrow(() -> new NotFoundException("coupon not found: " + code));
        coupon.deactivate();
    }

    public Coupon requireActive(String code) {
        return coupons.findByCode(code)
                .filter(Coupon::isActive)
                .orElseThrow(() -> new CouponException("coupon not found: " + code));
    }
}
