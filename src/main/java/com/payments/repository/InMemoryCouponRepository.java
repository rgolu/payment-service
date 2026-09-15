package com.payments.repository;

import com.payments.domain.model.Coupon;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCouponRepository implements CouponRepository {

    private final ConcurrentHashMap<String, Coupon> store = new ConcurrentHashMap<>();

    @Override
    public Coupon save(Coupon coupon) {
        store.put(coupon.getCode(), coupon);
        return coupon;
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return Optional.ofNullable(store.get(code.toUpperCase()));
    }
}
