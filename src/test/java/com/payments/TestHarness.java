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
import com.payments.wallet.WalletLedger;

import java.util.EnumMap;
import java.util.Map;

/** Builds a fully-wired in-memory graph so service tests do not need Spring. */
public final class TestHarness {

    public final ProviderRegistry providers = new ProviderRegistry();
    public final LoadFeeMultiplier load = new LoadFeeMultiplier();
    public final MethodFeePolicy fees = new MethodFeePolicy(DefaultFeeSchedules.all(), load);
    public final RoutingService routing;
    public final UserService users;
    public final MerchantService merchants;
    public final CouponService coupons;
    public final PaymentService payments;

    public TestHarness() {
        WalletLedger ledger = new WalletLedger();
        IdGenerator ids = new IdGenerator();
        Map<RoutingMode, RoutingStrategy> strategies = new EnumMap<>(RoutingMode.class);
        strategies.put(RoutingMode.FAILOVER, new FailoverRoutingStrategy(providers));
        strategies.put(RoutingMode.CHEAPEST, new CheapestRoutingStrategy(providers, fees));
        strategies.put(RoutingMode.SUCCESS_RATE, new SuccessRateRoutingStrategy(providers));
        this.routing = new RoutingService(strategies, RoutingMode.FAILOVER);
        this.users = new UserService(new InMemoryUserRepository(), ledger, ids);
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
                ids
        );
    }
}
