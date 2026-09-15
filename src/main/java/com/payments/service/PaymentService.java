package com.payments.service;

import com.payments.coupon.CouponEngine;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.PaymentStatus;
import com.payments.domain.exception.DuplicateEntityException;
import com.payments.domain.exception.InvalidStateException;
import com.payments.domain.exception.NotFoundException;
import com.payments.domain.model.Coupon;
import com.payments.domain.model.Merchant;
import com.payments.domain.model.Payment;
import com.payments.domain.model.Quote;
import com.payments.domain.model.User;
import com.payments.domain.money.Money;
import com.payments.fee.FeePolicy;
import com.payments.refund.RefundPolicy;
import com.payments.repository.PaymentRepository;
import com.payments.routing.RouteDecision;
import com.payments.routing.RoutingService;
import com.payments.wallet.LockExecutor;
import com.payments.wallet.WalletLedger;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates initiate → complete → refund. Strategies are injected;
 * this class must not know how fees or rails are chosen.
 */
public class PaymentService {

    private final UserService users;
    private final MerchantService merchants;
    private final CouponService coupons;
    private final PaymentRepository payments;
    private final RoutingService routing;
    private final FeePolicy fees;
    private final CouponEngine couponEngine;
    private final RefundPolicy refunds;
    private final WalletLedger ledger;
    private final LockExecutor locks;
    private final Clock clock;
    private final Duration pendingTtl;

    public PaymentService(
            UserService users,
            MerchantService merchants,
            CouponService coupons,
            PaymentRepository payments,
            RoutingService routing,
            FeePolicy fees,
            CouponEngine couponEngine,
            RefundPolicy refunds,
            WalletLedger ledger,
            LockExecutor locks,
            Clock clock,
            Duration pendingTtl
    ) {
        this.users = users;
        this.merchants = merchants;
        this.coupons = coupons;
        this.payments = payments;
        this.routing = routing;
        this.fees = fees;
        this.couponEngine = couponEngine;
        this.refunds = refunds;
        this.ledger = ledger;
        this.locks = locks;
        this.clock = clock;
        this.pendingTtl = pendingTtl;
    }

    public Payment initiate(
            String paymentId,
            String userId,
            String merchantId,
            Money amount,
            PaymentMethod requestedMethod,
            String couponCode
    ) {
        if (amount.isZero()) {
            throw new InvalidStateException("payment amount must be positive");
        }

        return locks.execute("idemp:" + paymentId, () -> {
            var existing = payments.findById(paymentId);
            if (existing.isPresent()) {
                Payment previous = expireIfDue(existing.get());
                if (previous.sameInitiateRequest(userId, merchantId, amount, requestedMethod, couponCode)) {
                    return previous;
                }
                throw new DuplicateEntityException(
                        "payment_id already exists with a different request: " + paymentId
                );
            }

            User user = users.get(userId);
            Merchant merchant = merchants.get(merchantId);
            RouteDecision route = routing.route(requestedMethod, merchant);

            Money discount = Money.ZERO;
            String appliedCode = null;
            Coupon coupon = null;
            if (couponCode != null && !couponCode.isBlank()) {
                coupon = coupons.requireActive(couponCode);
                discount = couponEngine.discount(coupon, amount, clock.instant());
                appliedCode = coupon.getCode();
            }

            Money principal = amount.minus(discount);
            Money fee = fees.quoteFee(principal, route.feeMethod());
            Money total = principal.plus(fee);
            Quote quote = new Quote(
                    amount,
                    discount,
                    principal,
                    fee,
                    total,
                    requestedMethod,
                    route.executedMethod(),
                    route.feeMethod(),
                    route.rerouted(),
                    appliedCode
            );

            Instant now = clock.instant();
            ledger.debitUser(user, total);
            if (coupon != null) {
                try {
                    synchronized (coupon) {
                        couponEngine.ensureApplicable(coupon, amount, now);
                        coupon.consumeUse();
                    }
                } catch (RuntimeException ex) {
                    ledger.creditUser(user, total);
                    throw ex;
                }
            }

            Payment payment = new Payment(paymentId, userId, merchantId, quote, now, now.plus(pendingTtl));
            return payments.save(payment);
        });
    }

    public Payment complete(String paymentId) {
        return locks.execute("idemp:" + paymentId, () -> {
            Payment payment = expireIfDue(get(paymentId));
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                return payment;
            }
            if (payment.getStatus() != PaymentStatus.PENDING) {
                throw new InvalidStateException("only PENDING payments can be completed");
            }
            Merchant merchant = merchants.get(payment.getMerchantId());
            ledger.creditMerchant(merchant, payment.getQuote().principal());
            payment.markCompleted(clock.instant());
            return payment;
        });
    }

    public Payment refund(String paymentId, String refundId) {
        return locks.execute("idemp:" + paymentId, () -> {
            Payment payment = expireIfDue(get(paymentId));
            if (payment.getStatus() == PaymentStatus.REFUNDED) {
                if (Objects.equals(payment.getRefundId(), refundId)) {
                    return payment;
                }
                throw new DuplicateEntityException("payment already refunded with a different refund_id");
            }
            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                throw new InvalidStateException("only COMPLETED payments can be refunded");
            }
            User user = users.get(payment.getUserId());
            Merchant merchant = merchants.get(payment.getMerchantId());
            Money principal = payment.getQuote().principal();
            Money refundFee = refunds.refundFee(principal);
            Money toUser = principal.minus(refundFee);

            ledger.debitMerchant(merchant, principal);
            ledger.creditUser(user, toUser);
            payment.markRefunded(refundId, refundFee, toUser, clock.instant());
            return payment;
        });
    }

    public Payment get(String paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("payment not found: " + paymentId));
        return expireIfDue(payment);
    }

    public List<Payment> historyForUser(String userId) {
        users.get(userId);
        return payments.findByUserId(userId).stream().map(this::expireIfDue).toList();
    }

    public List<Payment> historyForMerchant(String merchantId) {
        merchants.get(merchantId);
        return payments.findByMerchantId(merchantId).stream().map(this::expireIfDue).toList();
    }

    public int expireStale() {
        int expired = 0;
        for (Payment payment : payments.findPending()) {
            if (expireIfDue(payment).getStatus() == PaymentStatus.EXPIRED) {
                expired++;
            }
        }
        return expired;
    }

    private Payment expireIfDue(Payment payment) {
        if (!payment.isExpired(clock.instant())) {
            return payment;
        }
        return locks.execute("idemp:" + payment.getId(), () -> {
            if (!payment.isExpired(clock.instant())) {
                return payment;
            }
            User user = users.get(payment.getUserId());
            ledger.creditUser(user, payment.getQuote().total());
            payment.markExpired(clock.instant(), "pending payment expired; wallet hold released");
            return payment;
        });
    }
}
