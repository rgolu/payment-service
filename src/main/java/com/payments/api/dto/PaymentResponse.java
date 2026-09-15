package com.payments.api.dto;

import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.PaymentStatus;
import com.payments.domain.model.Payment;
import com.payments.domain.model.Quote;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String id,
        String userId,
        String merchantId,
        PaymentStatus status,
        BigDecimal originalAmount,
        BigDecimal discount,
        BigDecimal principal,
        BigDecimal fee,
        BigDecimal amountCharged,
        PaymentMethod requestedMethod,
        PaymentMethod executedMethod,
        PaymentMethod feeMethod,
        boolean rerouted,
        String couponCode,
        BigDecimal refundFee,
        BigDecimal refundedToUser,
        Instant createdAt,
        Instant completedAt,
        Instant refundedAt
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
                payment.getRefundFee() == null ? null : payment.getRefundFee().rupees(),
                payment.getRefundedToUser() == null ? null : payment.getRefundedToUser().rupees(),
                payment.getCreatedAt(),
                payment.getCompletedAt(),
                payment.getRefundedAt()
        );
    }
}
