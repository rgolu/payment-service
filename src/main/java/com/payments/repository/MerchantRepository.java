package com.payments.repository;

import com.payments.domain.model.Merchant;

import java.util.Optional;

public interface MerchantRepository {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(String id);
}
