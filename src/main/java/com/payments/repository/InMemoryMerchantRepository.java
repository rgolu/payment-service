package com.payments.repository;

import com.payments.domain.model.Merchant;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryMerchantRepository implements MerchantRepository {

    private final ConcurrentHashMap<String, Merchant> store = new ConcurrentHashMap<>();

    @Override
    public Merchant save(Merchant merchant) {
        store.put(merchant.getId(), merchant);
        return merchant;
    }

    @Override
    public Optional<Merchant> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
