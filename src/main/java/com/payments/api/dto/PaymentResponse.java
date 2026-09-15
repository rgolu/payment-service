package com.payments.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.PaymentStatus;
import com.payments.domain.model.Payment;
import com.payments.domain.model.Quote;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        @JsonProperty("payment_id") String paymentId,
        @JsonProperty("user_id") String userId,
        @JsonProperty("merchant_id") String merchantId,
        @JsonProperty("status") PaymentStatus status,
        @JsonProperty("original_amount") BigDecimal originalAmount,
        @JsonProperty("discount") BigDecimal discount,
        @JsonProperty("principal") BigDecimal principal,
        @JsonProperty("fee") BigDecimal fee,
        @JsonProperty("amount_charged") BigDecimal amountCharged,
        @JsonProperty("requested_method") PaymentMethod requestedMethod,
        @JsonProperty("executed_method") PaymentMethod executedMethod,
        @JsonProperty("fee_method") PaymentMethod feeMethod,
        @JsonProperty("rerouted") boolean rerouted,
        @JsonProperty("coupon_code") String couponCode,
        @JsonProperty("refund_id") String refundId,
        @JsonProperty("refund_fee") BigDecimal refundFee,
        @JsonProperty("refunded_to_user") BigDecimal refundedToUser,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("expires_at") Instant expiresAt,
        @JsonProperty("completed_at") Instant completedAt,
        @JsonProperty("refunded_at") Instant refundedAt,
        @JsonProperty("expired_at") Instant expiredAt
) {

    public static PaymentResponse from(Payment payment) {
        Quote q = payment.getQuote();
        return new PaymentResponse(
                payment.getId(),
                payment.getUserId(),
                payment.getMerchantId(),
                payment.getStatus(),
                q.originalAmount().rupees(),
                q.discount().rupees(),
                q.principal().rupees(),
                q.fee().rupees(),
                q.total().rupees(),
                q.requestedMethod(),
                q.executedMethod(),
                q.feeMethod(),
                q.rerouted(),
                q.couponCode(),
                payment.getRefundId(),
                payment.getRefundFee() == null ? null : payment.getRefundFee().rupees(),
                payment.getRefundedToUser() == null ? null : payment.getRefundedToUser().rupees(),
                payment.getCreatedAt(),
                payment.getExpiresAt(),
                payment.getCompletedAt(),
                payment.getRefundedAt(),
                payment.getExpiredAt()
        );
    }
}
