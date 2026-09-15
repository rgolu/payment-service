package com.payments.domain.model;

import com.payments.domain.enums.PaymentStatus;
import com.payments.domain.money.Money;

import java.time.Instant;

public class Payment {

    private final String id;
    private final String userId;
    private final String merchantId;
    private final Quote quote;
    private PaymentStatus status;
    private final Instant createdAt;
    private Instant completedAt;
    private Instant refundedAt;
    private Money refundFee;
    private Money refundedToUser;
    private String failureReason;

    public Payment(String id, String userId, String merchantId, Quote quote) {
        this.id = id;
        this.userId = userId;
        this.merchantId = merchantId;
        this.quote = quote;
        this.status = PaymentStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public Quote getQuote() {
        return quote;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public Money getRefundFee() {
        return refundFee;
    }

    public Money getRefundedToUser() {
        return refundedToUser;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Money amountCharged() {
        return quote.total();
    }

    public void markCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markRefunded(Money refundFee, Money refundedToUser) {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = Instant.now();
        this.refundFee = refundFee;
        this.refundedToUser = refundedToUser;
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }
}
