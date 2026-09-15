# Payment Service

In-memory Spring Boot backend for a Razorpay/Stripe-style payments platform.

```
Java 17 · Spring Boot 3.3 · Maven · no database
```

## Run

```bash
mvn test
mvn spring-boot:run
```

Server: `http://localhost:8080`. JSON is snake_case.

## API

| Capability | Endpoint |
|---|---|
| Register payer + wallet | `POST /api/users` |
| Top-up wallet | `POST /api/users/{user_id}/topup` |
| User history | `GET /api/users/{user_id}/transactions` |
| Register merchant + settlement + methods | `POST /api/merchants` |
| Merchant history | `GET /api/merchants/{merchant_id}/transactions` |
| Initiate payment (quote + debit) | `POST /api/payments/initiate` |
| Complete payment (credit merchant) | `POST /api/payments/{payment_id}/complete` |
| Refund | `POST /api/payments/{payment_id}/refund` |
| Add / delete coupon | `POST /api/coupons`, `DELETE /api/coupons/{code}` |
| Toggle UPI/Card availability | `POST /api/admin/providers/{method}/availability` |
| Switch routing strategy | `POST /api/admin/routing-mode` |
| Peak-hour fee multiplier | `POST /api/admin/load/{method}` |

Initiate requires a client `payment_id`. Top-up requires `credit_id`. Refund requires `refund_id`.

## Assumptions

1. **Money is `BigDecimal` scale 2 (HALF_UP).** `@Digits(integer = 12, fraction = 2)` on the wire. Scale-2 decimal is equivalent to integer paise if scale is always enforced.
2. **`payment_id` / `credit_id` / `refund_id` are idempotency keys.** Same key + same payload replays; same key + different payload is 409.
3. **PENDING expires after 15 minutes** (`payments.pending-ttl`). The wallet hold is released and status becomes `EXPIRED` (lazy on read + scheduled sweep).
4. **IDs** match `^[a-zA-Z0-9_.-]+$` (max 50). Names are alphanumeric with spaces. Amounts: 12 integer digits, 2 fraction digits.
5. **Locks are try-acquire (5s) and always released in `finally`.** A wallet mutation never runs without the lock.
6. **Initiate debits `principal + fee` and stores PENDING.** Complete credits the merchant with **principal only**. Fee is platform revenue.
7. **The spec fee example is the UPI schedule.** Card is more expensive (min ₹8; 2.5% / 2.0% / 1.5%) so “no extra fee” on UPI→Card reroute is a visible delta.
8. **Tiers are marginal.** ₹6,000 UPI = `2000×2% + 3000×1.5% + 1000×1% = ₹95`. Band `up_to` is inclusive.
9. **Min fee is a floor**, not an add-on. ₹100 UPI → `max(₹5, ₹2) = ₹5`.
10. **Coupon discounts the principal; fee is quoted on the discounted principal.** SAVE10 on ₹1,000 → ₹900 + ₹18 = ₹918.
11. **100% coupon → fee ₹0.** Min fee is not charged on zero principal.
12. **UPI downtime uses the Card rail at the UPI fee.** `executed_method=CARD`, `fee_method=UPI`.
13. **Merchant must support the requested method.** Card fallback also requires Card on the merchant. Card downtime has no fallback.
14. **Coupons:** `PERCENT` and `FLAT`, soft-delete, optional expiry / min amount / max cap / remaining uses. Use consumed on successful initiate.
15. **Refund:** only `COMPLETED`. Fee = `max(₹2, 1% of principal)`, capped at principal. User gets principal − refund fee. Original platform fee is kept. Settlement cannot go negative.
16. **Default routing is `FAILOVER`.** `CHEAPEST` and `SUCCESS_RATE` price the chosen rail (they do not keep free-reroute pricing).
17. **Load multiplier applies after** the tiered/min-fee quote.
18. **In-memory store is process-local.** Repositories are interfaces so a DB adapter can replace them without touching payment logic.

## Design

```
Controller  →  PaymentService (orchestrator)
                 ├── RoutingService        FAILOVER | CHEAPEST | SUCCESS_RATE
                 ├── FeePolicy             method schedule + optional load multiplier
                 ├── CouponEngine          PERCENT | FLAT
                 ├── RefundPolicy
                 └── WalletLedger          per-account lock via LockExecutor
```

- New fee tier → `DefaultFeeSchedules`.
- New payment method → enum + schedule + provider availability.
- New routing strategy → `RoutingStrategy` + register on `RoutingService`.
- New coupon type → `CouponEngine`.

`PaymentService` does not change for those. Domain objects are a plain Java graph wired in `AppConfig`; unit tests use `TestHarness` (no Spring).

Not built: a database, auth, webhooks, an event-sourced ledger.

## Trade-offs

| Choice | Why | Cost |
|---|---|---|
| Debit on initiate | Stops double-spend; quote is the charged amount | PENDING holds money until complete or expiry |
| Fee priced by `fee_method`, not `executed_method` | Free UPI→Card reroute is a routing decision | `CHEAPEST` / `SUCCESS_RATE` price the chosen rail |
| Soft-delete coupons | History stays explainable | Deleted codes are not reused |
| Per-key locks | Concurrent payments on different wallets proceed | Merchant settlement is locked on refund too |
| No Lombok | Every line is explicit | More boilerplate |

## Tests

- `FeeScheduleTest` — tiers, boundaries, min fee, rounding, zero principal
- `MethodFeePolicyTest` — method-specific rates, load multiplier
- `CouponEngineTest` — percent, flat, cap, deleted/expired/exhausted/min-amount
- `FailoverRoutingStrategyTest` — happy path, free UPI→Card reroute, no-fallback
- `PaymentServiceTest` — debit/credit, coupons, reroute fee, history, refund, idempotency, expiry
- `WalletConcurrencyTest` — 10 threads, ₹100 wallet, exactly 4 × ₹25 succeed
- `PaymentApiTest` — HTTP initiate/complete, 409 on insufficient balance, validation

## What I would do with more time

- Persist with a transactional store and an append-only ledger.
- Distributed lock (Redis) instead of in-process `ReentrantLock`.
- Split coupon calculators behind a strategy if a third type appears.
- Outbox + merchant webhooks on complete/refund.
- Property-based tests on the fee bands.

## How I used AI

I used Cursor to implement a Spring Boot service with strategy seams for fee, routing, and coupons; debit-on-initiate; request validation; and tests on the fee engine.

**Kept:** layering, `fee_method` vs `executed_method`, lock-around-debit, `TestHarness` so domain tests skip Spring.

**Rejected / rewrote:**
- Pricing a UPI→Card reroute with the Card schedule (would charge the user extra).
- Min-fee on a 100% coupon — flipped to fee ₹0 and documented it.
- A single large service class — harder to change fee or routing in isolation.
- JPA / event-sourcing — in-memory + repository interfaces is the cut for this exercise.
