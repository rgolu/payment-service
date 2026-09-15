package com.payments.repository;

import com.payments.domain.model.Payment;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPaymentRepository implements PaymentRepository {

    private final ConcurrentHashMap<String, Payment> store = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        store.put(payment.getId(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Payment> findByUserId(String userId) {
        return store.values().stream()
                .filter(p -> p.getUserId().equals(userId))
                .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public List<Payment> findByMerchantId(String merchantId) {
        return store.values().stream()
                .filter(p -> p.getMerchantId().equals(merchantId))
                .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                .toList();
    }
}
