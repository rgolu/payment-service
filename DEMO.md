# Demo script (30 minutes)

Open this file in split view. Run the server in a terminal:

```bash
mvn test          # ~10s, all green before you start talking
mvn spring-boot:run
```

Base URL: `http://localhost:8080`

---

## 0. Assumptions to say first (60 seconds)

- Amounts are **`BigDecimal` scale 2** (ppisvc `@Digits(12,2)`). Same safety as paise if scale is always enforced.
- JSON is **snake_case**. IDs match `^[a-zA-Z0-9_.-]+$`. Client supplies **`payment_id`** (idempotency, like PPI `debit_id`).
- **Initiate debits** `principal + fee`. **Complete credits** the merchant with **principal only**.
- Pending payments **expire in 15 minutes** and release the wallet hold.
- Spec fee bands = **UPI**. Card is costlier so reroute "at no extra fee" is visible.
- Coupon cuts **principal**, then fee is computed on that.
- Default routing is **FAILOVER** (UPI down → Card, still UPI price).

---

## 1. Working solution + one edge case each (8 min)

Save IDs from responses as you go.

### 1.1 Seed

```bash
curl -s -X POST localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Rishav","initial_balance":5000.00}' | jq

curl -s -X POST localhost:8080/api/merchants \
  -H 'Content-Type: application/json' \
  -d '{"name":"Cafe Mocha","supported_methods":["UPI","CARD"]}' | jq

curl -s -X POST localhost:8080/api/coupons \
  -H 'Content-Type: application/json' \
  -d '{"code":"SAVE10","type":"PERCENT","value":10,"remaining_uses":2}' | jq
```

### 1.2 Happy path — ₹1,000 UPI → fee ₹20, charged ₹1,020

```bash
curl -s -X POST localhost:8080/api/payments/initiate \
  -H 'Content-Type: application/json' \
  -d '{"payment_id":"pay_demo_1","user_id":"USR","merchant_id":"MER","amount":1000.00,"method":"UPI"}' | jq

curl -s -X POST localhost:8080/api/payments/pay_demo_1/complete | jq
# amount_charged = 1020, merchant settlement = 1000
```

**Talking point:** fee is platform revenue; merchant sees principal.

### 1.3 Edge — minimum fee (₹100 UPI → fee ₹5, not ₹2)

```bash
curl -s -X POST localhost:8080/api/payments/initiate \
  -H 'Content-Type: application/json' \
  -d '{"payment_id":"pay_min","user_id":"USR","merchant_id":"MER","amount":100.00,"method":"UPI"}' | jq
# fee = 5, amount_charged = 105
```

### 1.4 Edge — insufficient balance (no side effects)

```bash
curl -s -X POST localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Broke","initial_balance":10.00}' | jq

curl -s -X POST localhost:8080/api/payments/initiate \
  -H 'Content-Type: application/json' \
  -d '{"payment_id":"pay_broke","user_id":"BROKE","merchant_id":"MER","amount":1000.00,"method":"UPI"}' | jq
# 409 InsufficientBalanceException; wallet still 10; no payment row
```

### 1.5 Edge — UPI downtime, free Card reroute

```bash
curl -s -X POST localhost:8080/api/admin/providers/UPI/availability \
  -H 'Content-Type: application/json' \
  -d '{"available":false}' | jq

curl -s -X POST localhost:8080/api/payments/initiate \
  -H 'Content-Type: application/json' \
  -d '{"payment_id":"pay_reroute","user_id":"USR","merchant_id":"MER","amount":1000.00,"method":"UPI"}' | jq
# rerouted=true, executed_method=CARD, fee_method=UPI, fee=20 (not Card's 25)
```

Turn UPI back on before the coupon step:

```bash
curl -s -X POST localhost:8080/api/admin/providers/UPI/availability \
  -H 'Content-Type: application/json' \
  -d '{"available":true}'
```

### 1.6 Edge — coupon, then invalid coupon

```bash
curl -s -X POST localhost:8080/api/payments/initiate \
  -H 'Content-Type: application/json' \
  -d '{"payment_id":"pay_coupon","user_id":"USR","merchant_id":"MER","amount":1000.00,"method":"UPI","coupon_code":"SAVE10"}' | jq
# discount=100, principal=900, fee=18, amount_charged=918

curl -s -X POST localhost:8080/api/payments/initiate \
  -H 'Content-Type: application/json' \
  -d '{"payment_id":"pay_bad","user_id":"USR","merchant_id":"MER","amount":1000.00,"method":"UPI","coupon_code":"NOPE"}' | jq
# 400 CouponException
```

### 1.7 History

```bash
curl -s localhost:8080/api/users/USR/transactions | jq
curl -s localhost:8080/api/merchants/MER/transactions | jq
```

---

## 2. Design walkthrough (8 min)

They will pick files. Have these ready:

| If they ask… | Open |
|---|---|
| Layering | `PaymentService.java` — only orchestration |
| Fee tiers / min fee | `FeeSchedule.java` + `DefaultFeeSchedules.java` |
| Method rates + load | `MethodFeePolicy.java`, `LoadFeeMultiplier.java` |
| UPI→Card free reroute | `FailoverRoutingStrategy.java` (`feeMethod=UPI`) |
| Switchable routing | `RoutingService.java` + `AppConfig` |
| Coupons | `CouponEngine.java` |
| Concurrency | `WalletLedger.java` |
| Money | `Money.java` |
| Why tests skip Spring | `TestHarness.java` |

**Say this once:** *A new fee strategy, payment method, or routing strategy lands in one place. PaymentService does not change.*

State machine:

```
initiate → PENDING (wallet debited)
complete → COMPLETED (merchant credited principal)
refund   → REFUNDED (merchant debited principal, user credited principal − refund fee)
```

---

## 3. Live extension (10 min) — how to scope it

They will ask for one of these. Do **only** that change, then run the matching test.

### A. New fee tier (e.g. ₹10,001+ at 0.75%)

1. `DefaultFeeSchedules` — insert `new FeeTier(Money.rupees("10000"), 100)` and change the last tier to `75` bps.
2. Add one assert in `FeeScheduleTest` for ₹12,000.
3. `mvn -q -Dtest=FeeScheduleTest test`

### B. NetBanking payment method

1. `PaymentMethod.NETBANKING`
2. `DefaultFeeSchedules` — add a schedule; put it in `all()`
3. `ProviderRegistry` constructor — mark it up
4. Register a merchant with `["UPI","CARD","NETBANKING"]` and initiate
5. Optional: one test that NetBanking fee ≠ UPI fee

Failover does **not** auto-fallback to NetBanking (spec only named Card). Say that out loud.

### C. New coupon type (e.g. `FLAT` is already here — they may ask for `CASHBACK` or `PERCENT_ON_FEE`)

`FLAT` is already implemented so you can show the seam. If they want a **new** type:

1. `CouponType.CASHBACK` (or whatever they said)
2. One `if` in `CouponEngine.discount`
3. A test in `CouponEngineTest`
4. API already accepts `type` as the enum

**Do not** touch `PaymentService` for A/B/C.

After the change: `mvn -q test` or the single test class.

---

## 4. Q&A cheat sheet (4 min)

**Why debit on initiate?** So two concurrent initiates cannot both pass a balance check. `WalletConcurrencyTest` is the proof (10 threads, 4 successes, wallet 0).

**Why `feeMethod` ≠ `executedMethod`?** Pricing is a policy. Rail is a routing decision. Mixing them makes "free reroute" leak into the fee engine.

**Why no JPA?** Spec allows in-memory. Repositories are interfaces; a JPA adapter is mechanical.

**What happens to the fee on refund?** Kept. Refund fee is a second policy (`RefundPolicy`). User does not get the original MDR back.

**Could cheapest routing still honour "no extra fee"?** Yes — pass `requested` as `feeMethod` when `requested == UPI && executed == CARD`. I did **not**, because cheapest is an optimiser, not a failover. Happy to add a one-liner if they want it live.

**Idempotency?** Client `payment_id` (and `credit_id` / `refund_id`). Same payload replays; different payload is 409. Same pattern as PPI `debit_id`.

**PENDING forever?** No. TTL 15 minutes, then `EXPIRED` and the wallet hold is released (lazy + scheduled sweep).

---

## Fee crib sheet (don't recompute live)

| Amount | UPI fee | Card fee |
|---|---|---|
| ₹100 | ₹5 (min) | ₹8 (min) |
| ₹1,000 | ₹20 | ₹25 |
| ₹2,000 | ₹40 | ₹50 |
| ₹2,001 | ₹40.02 | ₹50.02 |
| ₹5,000 | ₹85 | ₹110 |
| ₹6,000 | ₹95 | ₹125 |
| ₹1,000 + SAVE10 | principal ₹900, UPI fee ₹18, total ₹918 | |
