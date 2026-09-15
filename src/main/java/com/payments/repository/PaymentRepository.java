package com.payments.repository;

import com.payments.domain.model.Payment;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(String id);

    List<Payment> findByUserId(String userId);

    List<Payment> findByMerchantId(String merchantId);
}
