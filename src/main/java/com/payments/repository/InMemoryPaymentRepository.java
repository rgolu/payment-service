package com.payments.repository;

import com.payments.domain.enums.PaymentStatus;
import com.payments.domain.model.Payment;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Primary store is keyed by payment_id (O(1) idempotency lookup).
 * Secondary indexes keep history/expiry off the full-scan path.
 */
public class InMemoryPaymentRepository implements PaymentRepository {

    private final ConcurrentHashMap<String, Payment> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Payment>> byUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Payment>> byMerchant = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Payment> pending = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        store.put(payment.getId(), payment);
        byUser.computeIfAbsent(payment.getUserId(), k -> ConcurrentHashMap.newKeySet()).add(payment);
        byMerchant.computeIfAbsent(payment.getMerchantId(), k -> ConcurrentHashMap.newKeySet()).add(payment);
        if (payment.getStatus() == PaymentStatus.PENDING) {
            pending.put(payment.getId(), payment);
        } else {
            pending.remove(payment.getId());
        }
        return payment;
    }

    @Override
    public Optional<Payment> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Payment> findByUserId(String userId) {
        return byUser.getOrDefault(userId, Set.of()).stream()
                .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public List<Payment> findByMerchantId(String merchantId) {
        return byMerchant.getOrDefault(merchantId, Set.of()).stream()
                .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public List<Payment> findPending() {
        return List.copyOf(pending.values());
    }
}
