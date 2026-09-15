package com.payments.service;

import com.payments.coupon.CouponEngine;
import com.payments.domain.enums.PaymentMethod;
import com.payments.domain.enums.PaymentStatus;
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
import com.payments.wallet.WalletLedger;

import java.time.Instant;
import java.util.List;

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
    private final IdGenerator ids;

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
            IdGenerator ids
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
        this.ids = ids;
    }

    public Payment initiate(
            String userId,
            String merchantId,
            Money amount,
            PaymentMethod requestedMethod,
            String couponCode
    ) {
        if (amount.isZero()) {
            throw new InvalidStateException("payment amount must be positive");
        }

        User user = users.get(userId);
        Merchant merchant = merchants.get(merchantId);
        RouteDecision route = routing.route(requestedMethod, merchant);

        Money discount = Money.ZERO;
        String appliedCode = null;
        Coupon coupon = null;
        if (couponCode != null && !couponCode.isBlank()) {
            coupon = coupons.requireActive(couponCode);
            discount = couponEngine.discount(coupon, amount, Instant.now());
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

        // Debit first so a later coupon-consume failure can still be rolled back.
        ledger.debitUser(user, total);
        if (coupon != null) {
            try {
                synchronized (coupon) {
                    couponEngine.ensureApplicable(coupon, amount, Instant.now());
                    coupon.consumeUse();
                }
            } catch (RuntimeException ex) {
                ledger.creditUser(user, total);
                throw ex;
            }
        }

        Payment payment = new Payment(ids.paymentId(), userId, merchantId, quote);
        return payments.save(payment);
    }

    public Payment complete(String paymentId) {
        Payment payment = get(paymentId);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidStateException("only PENDING payments can be completed");
        }
        Merchant merchant = merchants.get(payment.getMerchantId());
        ledger.creditMerchant(merchant, payment.getQuote().principal());
        payment.markCompleted();
        return payment;
    }

    public Payment refund(String paymentId) {
        Payment payment = get(paymentId);
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidStateException("only COMPLETED payments can be refunded");
        }
        User user = users.get(payment.getUserId());
        Merchant merchant = merchants.get(payment.getMerchantId());
        Money principal = payment.getQuote().principal();
        Money refundFee = refunds.refundFee(principal);
        Money toUser = principal.minus(refundFee);

        // Merchant first: if settlement is short we must not credit the user.
        ledger.debitMerchant(merchant, principal);
        ledger.creditUser(user, toUser);
        payment.markRefunded(refundFee, toUser);
        return payment;
    }

    public Payment get(String paymentId) {
        return payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("payment not found: " + paymentId));
    }

    public List<Payment> historyForUser(String userId) {
        users.get(userId);
        return payments.findByUserId(userId);
    }

    public List<Payment> historyForMerchant(String merchantId) {
        merchants.get(merchantId);
        return payments.findByMerchantId(merchantId);
    }
}
