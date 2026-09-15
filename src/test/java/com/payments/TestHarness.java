package com.payments;

import com.payments.coupon.CouponEngine;
import com.payments.domain.enums.RoutingMode;
import com.payments.fee.DefaultFeeSchedules;
import com.payments.fee.LoadFeeMultiplier;
import com.payments.fee.MethodFeePolicy;
import com.payments.provider.ProviderRegistry;
import com.payments.refund.RefundPolicy;
import com.payments.repository.InMemoryCouponRepository;
import com.payments.repository.InMemoryMerchantRepository;
import com.payments.repository.InMemoryPaymentRepository;
import com.payments.repository.InMemoryUserRepository;
import com.payments.routing.CheapestRoutingStrategy;
import com.payments.routing.FailoverRoutingStrategy;
import com.payments.routing.RoutingService;
import com.payments.routing.RoutingStrategy;
import com.payments.routing.SuccessRateRoutingStrategy;
import com.payments.service.CouponService;
import com.payments.service.IdGenerator;
import com.payments.service.MerchantService;
import com.payments.service.PaymentService;
import com.payments.service.UserService;
import com.payments.wallet.LockExecutor;
import com.payments.wallet.WalletLedger;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Builds a fully-wired in-memory graph so service tests do not need Spring. */
public final class TestHarness {

    public final AdjustableClock clock = new AdjustableClock();
    public final ProviderRegistry providers = new ProviderRegistry();
    public final LoadFeeMultiplier load = new LoadFeeMultiplier();
    public final MethodFeePolicy fees = new MethodFeePolicy(DefaultFeeSchedules.all(), load);
    public final RoutingService routing;
    public final UserService users;
    public final MerchantService merchants;
    public final CouponService coupons;
    public final PaymentService payments;
    private final AtomicInteger seq = new AtomicInteger();

    public TestHarness() {
        this(Duration.ofMinutes(15));
    }

    public TestHarness(Duration pendingTtl) {
        LockExecutor locks = new LockExecutor(Duration.ofSeconds(5));
        WalletLedger ledger = new WalletLedger(locks);
        IdGenerator ids = new IdGenerator();
        Map<RoutingMode, RoutingStrategy> strategies = new EnumMap<>(RoutingMode.class);
        strategies.put(RoutingMode.FAILOVER, new FailoverRoutingStrategy(providers));
        strategies.put(RoutingMode.CHEAPEST, new CheapestRoutingStrategy(providers, fees));
        strategies.put(RoutingMode.SUCCESS_RATE, new SuccessRateRoutingStrategy(providers));
        this.routing = new RoutingService(strategies, RoutingMode.FAILOVER);
        this.users = new UserService(new InMemoryUserRepository(), ledger, locks, ids);
        this.merchants = new MerchantService(new InMemoryMerchantRepository(), ids);
        this.coupons = new CouponService(new InMemoryCouponRepository());
        this.payments = new PaymentService(
                users,
                merchants,
                coupons,
                new InMemoryPaymentRepository(),
                routing,
                fees,
                new CouponEngine(),
                new RefundPolicy(),
                ledger,
                locks,
                clock,
                pendingTtl
        );
    }

    public String nextPaymentId() {
        return "pay_" + seq.incrementAndGet();
    }
}
