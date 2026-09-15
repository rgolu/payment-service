package com.payments.config;

import com.payments.coupon.CouponEngine;
import com.payments.domain.enums.RoutingMode;
import com.payments.fee.DefaultFeeSchedules;
import com.payments.fee.FeePolicy;
import com.payments.fee.LoadFeeMultiplier;
import com.payments.fee.MethodFeePolicy;
import com.payments.provider.ProviderRegistry;
import com.payments.refund.RefundPolicy;
import com.payments.repository.CouponRepository;
import com.payments.repository.InMemoryCouponRepository;
import com.payments.repository.InMemoryMerchantRepository;
import com.payments.repository.InMemoryPaymentRepository;
import com.payments.repository.InMemoryUserRepository;
import com.payments.repository.MerchantRepository;
import com.payments.repository.PaymentRepository;
import com.payments.repository.UserRepository;
import com.payments.routing.CheapestRoutingStrategy;
import com.payments.routing.FailoverRoutingStrategy;
import com.payments.routing.RoutingService;
import com.payments.routing.RoutingStrategy;
import com.payments.routing.SuccessRateRoutingStrategy;
import com.payments.service.CouponService;
import com.payments.service.IdGenerator;
import com.payments.service.MerchantService;
import com.payments.service.PaymentService;
import com.payments.service.PendingPaymentSweeper;
import com.payments.service.UserService;
import com.payments.wallet.LockExecutor;
import com.payments.wallet.WalletLedger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public LockExecutor lockExecutor(@Value("${payments.lock-timeout:5s}") Duration lockTimeout) {
        return new LockExecutor(lockTimeout);
    }

    @Bean
    public UserRepository userRepository() {
        return new InMemoryUserRepository();
    }

    @Bean
    public MerchantRepository merchantRepository() {
        return new InMemoryMerchantRepository();
    }

    @Bean
    public PaymentRepository paymentRepository() {
        return new InMemoryPaymentRepository();
    }

    @Bean
    public CouponRepository couponRepository() {
        return new InMemoryCouponRepository();
    }

    @Bean
    public WalletLedger walletLedger(LockExecutor locks) {
        return new WalletLedger(locks);
    }

    @Bean
    public IdGenerator idGenerator() {
        return new IdGenerator();
    }

    @Bean
    public ProviderRegistry providerRegistry() {
        return new ProviderRegistry();
    }

    @Bean
    public LoadFeeMultiplier loadFeeMultiplier() {
        return new LoadFeeMultiplier();
    }

    @Bean
    public FeePolicy feePolicy(LoadFeeMultiplier loadFeeMultiplier) {
        return new MethodFeePolicy(DefaultFeeSchedules.all(), loadFeeMultiplier);
    }

    @Bean
    public CouponEngine couponEngine() {
        return new CouponEngine();
    }

    @Bean
    public RefundPolicy refundPolicy() {
        return new RefundPolicy();
    }

    @Bean
    public RoutingService routingService(
            ProviderRegistry providers,
            FeePolicy feePolicy,
            @Value("${payments.routing-mode:FAILOVER}") RoutingMode mode
    ) {
        Map<RoutingMode, RoutingStrategy> strategies = new EnumMap<>(RoutingMode.class);
        strategies.put(RoutingMode.FAILOVER, new FailoverRoutingStrategy(providers));
        strategies.put(RoutingMode.CHEAPEST, new CheapestRoutingStrategy(providers, feePolicy));
        strategies.put(RoutingMode.SUCCESS_RATE, new SuccessRateRoutingStrategy(providers));
        return new RoutingService(strategies, mode);
    }

    @Bean
    public UserService userService(UserRepository users, WalletLedger ledger, LockExecutor locks, IdGenerator ids) {
        return new UserService(users, ledger, locks, ids);
    }

    @Bean
    public MerchantService merchantService(MerchantRepository merchants, IdGenerator ids) {
        return new MerchantService(merchants, ids);
    }

    @Bean
    public CouponService couponService(CouponRepository coupons) {
        return new CouponService(coupons);
    }

    @Bean
    public PaymentService paymentService(
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
            @Value("${payments.pending-ttl:15m}") Duration pendingTtl
    ) {
        return new PaymentService(
                users, merchants, coupons, payments, routing, fees, couponEngine, refunds, ledger, locks, clock, pendingTtl
        );
    }

    @Bean
    public PendingPaymentSweeper pendingPaymentSweeper(PaymentService payments) {
        return new PendingPaymentSweeper(payments);
    }
}
