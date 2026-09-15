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
    private final Instant expiresAt;
    private Instant completedAt;
    private Instant refundedAt;
    private Instant expiredAt;
    private Money refundFee;
    private Money refundedToUser;
    private String refundId;
    private String failureReason;

    public Payment(String id, String userId, String merchantId, Quote quote, Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.merchantId = merchantId;
        this.quote = quote;
        this.status = PaymentStatus.PENDING;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public Money getRefundFee() {
        return refundFee;
    }

    public Money getRefundedToUser() {
        return refundedToUser;
    }

    public String getRefundId() {
        return refundId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Money amountCharged() {
        return quote.total();
    }

    public boolean isExpired(Instant now) {
        return status == PaymentStatus.PENDING && !now.isBefore(expiresAt);
    }

    public void markCompleted(Instant now) {
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = now;
    }

    public void markRefunded(String refundId, Money refundFee, Money refundedToUser, Instant now) {
        this.status = PaymentStatus.REFUNDED;
        this.refundId = refundId;
        this.refundedAt = now;
        this.refundFee = refundFee;
        this.refundedToUser = refundedToUser;
    }

    public void markExpired(Instant now, String reason) {
        this.status = PaymentStatus.EXPIRED;
        this.expiredAt = now;
        this.failureReason = reason;
    }

    public void markFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public boolean sameInitiateRequest(String userId, String merchantId, Money amount, Object method, String couponCode) {
        String applied = couponCode == null || couponCode.isBlank() ? null : couponCode.trim().toUpperCase();
        return this.userId.equals(userId)
                && this.merchantId.equals(merchantId)
                && this.quote.originalAmount().equals(amount)
                && this.quote.requestedMethod().name().equals(method.toString())
                && java.util.Objects.equals(this.quote.couponCode(), applied);
    }
}
