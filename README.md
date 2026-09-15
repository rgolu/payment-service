# Payment Service

In-memory **Spring Boot** backend for a Razorpay/Stripe-style payments platform. Built as an AI-assisted machine-coding exercise: working code is the baseline; the interesting part is where the seams are and what was decided when the spec was silent.

```
Java 17 · Spring Boot 3.3 · Maven · no database
```

## Run

```bash
mvn test
mvn spring-boot:run
```

Server listens on `http://localhost:8080`. A timed walkthrough (including curl) is in [`DEMO.md`](DEMO.md).

## What it does

| Capability | Endpoint |
|---|---|
| Register payer + wallet | `POST /api/users` |
| Top-up wallet | `POST /api/users/{id}/topup` |
| User history (pending + completed) | `GET /api/users/{id}/transactions` |
| Register merchant + settlement + methods | `POST /api/merchants` |
| Merchant history | `GET /api/merchants/{id}/transactions` |
| Initiate payment (quote + debit) | `POST /api/payments/initiate` |
| Complete payment (credit merchant) | `POST /api/payments/{id}/complete` |
| Refund (bonus) | `POST /api/payments/{id}/refund` |
| Add / delete coupon | `POST /api/coupons`, `DELETE /api/coupons/{code}` |
| Toggle UPI/Card downtime | `POST /api/admin/providers/{method}/availability` |
| Switch routing strategy | `POST /api/admin/routing-mode` |
| Peak-hour fee multiplier | `POST /api/admin/load/{method}` |

## Assumptions (spec was silent — I decided)

1. **Money is integer paise.** No `double`. Half-up to the nearest paisa.
2. **Two-phase payment.** `initiate` computes the full quote (principal + fee), **debits the user immediately**, and stores `PENDING`. `complete` **credits the merchant with the principal only**; the fee is platform revenue. This prevents overspend and makes "amount charged" deterministic at initiate time.
3. **Fee example is the UPI schedule.** Card is more expensive (min ₹8; 2.5% / 2.0% / 1.5%) so "no extra fee" on UPI→Card reroute is a real, testable delta.
4. **Tiers are marginal, not flat.** ₹6,000 UPI = `2000×2% + 3000×1.5% + 1000×1% = ₹95`. The `up_to` bound is inclusive of that band.
5. **Minimum fee is a floor on the computed fee**, not an add-on. ₹100 UPI → max(₹5, ₹2) = ₹5.
6. **Coupon discounts the principal, then fee is quoted on the discounted principal.** A 10% coupon on ₹1,000 → principal ₹900, UPI fee ₹18, charged ₹918.
7. **100% coupon → fee ₹0.** Charging a ₹5 min fee on a zero-principal payment felt punitive; documented so it can be flipped in `FeeSchedule`.
8. **UPI downtime reroute prices the UPI schedule on the Card rail.** `executedMethod=CARD`, `feeMethod=UPI`. User never pays the Card premium.
9. **Merchant must support the requested method.** Fallback to Card also requires the merchant to accept Card. Card downtime has no fallback (spec only described UPI→Card).
10. **Coupons:** `PERCENT` (1–100) and `FLAT` (rupees). Soft-delete. Optional expiry, min-amount, max-discount, remaining uses. Usage is consumed on successful initiate.
11. **Refund (bonus):** only `COMPLETED`. Fee = `max(₹2, 1% of principal)`, capped at principal. User gets `principal − refundFee`. Merchant is debited the full principal. Original platform fee is kept.
12. **Settlement cannot go negative.** A refund is rejected if the merchant has already been paid out / has insufficient settlement.
13. **Default routing is `FAILOVER`.** `CHEAPEST` and `SUCCESS_RATE` are switchable at runtime and do **not** keep the "free reroute" pricing (they price the chosen rail). That is an explicit trade-off: those modes optimise cost/reliability, not the user's requested-method fee.
14. **Load multiplier** applies *after* the tiered/min-fee computation. ₹20 UPI fee at 1.2× load = ₹24.
15. **In-memory store is process-local.** Restart loses data. Repositories are interfaces so a DB adapter can replace them without touching payment logic.

## Design — where the seams are

```
Controller  →  PaymentService (orchestrator)
                 ├── RoutingService        Strategy: FAILOVER | CHEAPEST | SUCCESS_RATE
                 ├── FeePolicy             Method schedule + optional LoadFeeMultiplier
                 ├── CouponEngine          PERCENT | FLAT
                 ├── RefundPolicy
                 └── WalletLedger          per-user / per-merchant ReentrantLock
```

**A new fee tier** lands in `DefaultFeeSchedules` (one list).  
**A new payment method** = enum value + `FeeSchedule` + `ProviderRegistry.register`.  
**A new routing strategy** = implement `RoutingStrategy`, register it on `RoutingService`.  
**A new coupon type** = a branch in `CouponEngine` (or a `CouponCalculator` if a third type appears).  
Payment orchestration does not change for any of the above.

I used **constructor-injected interfaces and a manual `AppConfig`** instead of `@Service` on every class so the domain stays a plain Java graph. Tests construct it with `TestHarness` — no Spring context for fee/routing/payment tests.

What I chose **not** to build: a database, auth, webhooks, idempotency keys, ledger entries as an event log, distributed transactions. Those are the right production next steps; they would hide the design in this exercise.

## Trade-offs

| Choice | Why | Cost |
|---|---|---|
| Debit on initiate | Stops double-spend; quote is the charged amount | Pending payments "hold" money; need a timeout/expire job in production |
| Fee priced by `feeMethod`, not `executedMethod` | Makes free UPI→Card reroute a one-line decision in routing | `CHEAPEST` / `SUCCESS_RATE` do not reuse that policy (by design) |
| Soft-delete coupons | History stays explainable | Deleted codes cannot be reused until we add a unique-active constraint |
| Per-key locks, not a global lock | Concurrent payments on different wallets proceed | Must remember to lock merchant settlement too on refund |
| No Lombok | Every line is visible and ownable in a live review | More boilerplate |

## Tests

Mandatory fee-engine coverage lives in:

- `FeeScheduleTest` — tiers, boundaries, min fee, rounding, zero principal
- `MethodFeePolicyTest` — method-specific rates, load multiplier
- `CouponEngineTest` — percent, flat, cap, deleted/expired/exhausted/min-amount
- `FailoverRoutingStrategyTest` — happy path, free UPI→Card reroute, no-fallback cases
- `PaymentServiceTest` — debit/credit, insufficient balance, coupon on quote, invalid/deleted coupon, reroute fee, history, refund
- `WalletConcurrencyTest` — 10 threads, ₹100 wallet, ₹25 quotes → exactly 4 succeed, wallet = 0
- `PaymentApiTest` — HTTP initiate/complete + 409 on insufficient balance

## What I would do with more time

- Persist with a transactional store and an append-only ledger.
- Idempotency keys on initiate/complete.
- Expire stale `PENDING` payments and release the hold.
- Split coupon calculators behind a strategy (the live-extension hint).
- Outbox + merchant webhooks on complete/refund.
- Property-based tests on the fee bands.

## How I used AI

I directed Cursor to implement a **Spring Boot** service with strategy seams for fee, routing, and coupons; integer money; debit-on-initiate; and tests first on the fee engine.

**Kept:** layering, `Money` as paise, `feeMethod` vs `executedMethod`, `WalletLedger` locks, `TestHarness` so domain tests skip Spring.

**Rejected / rewrote:**
- Python first pass — wrong language for this submission.
- Pricing reroutes with the Card schedule — that would charge the user extra, which the spec forbids.
- Min-fee on a 100% coupon — felt wrong; flipped to fee ₹0 and wrote it down.
- Lombok / a giant `PaymentServiceImpl` — harder to own in a 10-minute live extension.
- Event-sourcing / JPA — scope theatre. In-memory + repository interfaces is the honest cut.

If I cannot explain a line in the walkthrough, it should not be in this repo.
