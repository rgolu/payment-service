#!/usr/bin/env bash
# Scripted happy-path + edge cases for the 8-minute demo.
# Usage: start the server (`mvn spring-boot:run`) then `bash demo.sh`
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"

json() { curl -sS -X "$1" "$BASE$2" -H 'Content-Type: application/json' ${3:+-d "$3"}; echo; }

echo "== register =="
USER=$(json POST /api/users '{"name":"Rishav","initial_balance":5000.00}')
MERCH=$(json POST /api/merchants '{"name":"Cafe Mocha","supported_methods":["UPI","CARD"]}')
json POST /api/coupons '{"code":"SAVE10","type":"PERCENT","value":10,"remaining_uses":2}' >/dev/null
USER_ID=$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['user_id'])" "$USER")
MERCH_ID=$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['merchant_id'])" "$MERCH")
echo "user=$USER_ID merchant=$MERCH_ID"

echo "== happy path ₹1000 UPI =="
PAY=$(json POST /api/payments/initiate "{\"payment_id\":\"pay_demo_1\",\"user_id\":\"$USER_ID\",\"merchant_id\":\"$MERCH_ID\",\"amount\":1000.00,\"method\":\"UPI\"}")
PAY_ID=$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['payment_id'])" "$PAY")
json POST "/api/payments/$PAY_ID/complete"

echo "== min fee ₹100 UPI =="
json POST /api/payments/initiate "{\"payment_id\":\"pay_demo_2\",\"user_id\":\"$USER_ID\",\"merchant_id\":\"$MERCH_ID\",\"amount\":100.00,\"method\":\"UPI\"}"

echo "== insufficient balance =="
BROKE=$(json POST /api/users '{"name":"Broke","initial_balance":10.00}')
BROKE_ID=$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['user_id'])" "$BROKE")
json POST /api/payments/initiate "{\"payment_id\":\"pay_broke\",\"user_id\":\"$BROKE_ID\",\"merchant_id\":\"$MERCH_ID\",\"amount\":1000.00,\"method\":\"UPI\"}" || true

echo "== UPI downtime free reroute =="
json POST /api/admin/providers/UPI/availability '{"available":false}'
json POST /api/payments/initiate "{\"payment_id\":\"pay_reroute\",\"user_id\":\"$USER_ID\",\"merchant_id\":\"$MERCH_ID\",\"amount\":1000.00,\"method\":\"UPI\"}"
json POST /api/admin/providers/UPI/availability '{"available":true}'

echo "== coupon SAVE10 =="
json POST /api/payments/initiate "{\"payment_id\":\"pay_coupon\",\"user_id\":\"$USER_ID\",\"merchant_id\":\"$MERCH_ID\",\"amount\":1000.00,\"method\":\"UPI\",\"coupon_code\":\"SAVE10\"}"

echo "== invalid coupon =="
json POST /api/payments/initiate "{\"payment_id\":\"pay_bad_coupon\",\"user_id\":\"$USER_ID\",\"merchant_id\":\"$MERCH_ID\",\"amount\":1000.00,\"method\":\"UPI\",\"coupon_code\":\"NOPE\"}" || true

echo "== idempotent replay =="
json POST /api/payments/initiate "{\"payment_id\":\"pay_demo_1\",\"user_id\":\"$USER_ID\",\"merchant_id\":\"$MERCH_ID\",\"amount\":1000.00,\"method\":\"UPI\"}"

echo "== history =="
json GET "/api/users/$USER_ID/transactions"
json GET "/api/merchants/$MERCH_ID/transactions"
