package com.payments.repository;

import com.payments.domain.model.Coupon;

import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findByCode(String code);
}
