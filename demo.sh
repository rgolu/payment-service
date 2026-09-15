#!/usr/bin/env bash
# Scripted happy-path + edge cases for the 8-minute demo.
# Usage: start the server (`mvn spring-boot:run`) then `bash demo.sh`
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"

json() { curl -sS -X "$1" "$BASE$2" -H 'Content-Type: application/json' ${3:+-d "$3"}; echo; }

echo "== register =="
USER=$(json POST /api/users '{"name":"Rishav","initialBalance":5000}')
MERCH=$(json POST /api/merchants '{"name":"Cafe Mocha","supportedMethods":["UPI","CARD"]}')
json POST /api/coupons '{"code":"SAVE10","type":"PERCENT","value":10,"remainingUses":2}' >/dev/null
USER_ID=$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['id'])" "$USER")
MERCH_ID=$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['id'])" "$MERCH")
echo "user=$USER_ID merchant=$MERCH_ID"

echo "== happy path ₹1000 UPI =="
PAY=$(json POST /api/payments/initiate "{\"userId\":\"$USER_ID\",\"merchantId\":\"$MERCH_ID\",\"amount\":1000,\"method\":\"UPI\"}")
PAY_ID=$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['id'])" "$PAY")
json POST "/api/payments/$PAY_ID/complete"

echo "== min fee ₹100 UPI =="
json POST /api/payments/initiate "{\"userId\":\"$USER_ID\",\"merchantId\":\"$MERCH_ID\",\"amount\":100,\"method\":\"UPI\"}"

echo "== insufficient balance =="
BROKE=$(json POST /api/users '{"name":"Broke","initialBalance":10}')
BROKE_ID=$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['id'])" "$BROKE")
json POST /api/payments/initiate "{\"userId\":\"$BROKE_ID\",\"merchantId\":\"$MERCH_ID\",\"amount\":1000,\"method\":\"UPI\"}" || true

echo "== UPI downtime free reroute =="
json POST /api/admin/providers/UPI/availability '{"available":false}'
json POST /api/payments/initiate "{\"userId\":\"$USER_ID\",\"merchantId\":\"$MERCH_ID\",\"amount\":1000,\"method\":\"UPI\"}"
json POST /api/admin/providers/UPI/availability '{"available":true}'

echo "== coupon SAVE10 =="
json POST /api/payments/initiate "{\"userId\":\"$USER_ID\",\"merchantId\":\"$MERCH_ID\",\"amount\":1000,\"method\":\"UPI\",\"couponCode\":\"SAVE10\"}"

echo "== invalid coupon =="
json POST /api/payments/initiate "{\"userId\":\"$USER_ID\",\"merchantId\":\"$MERCH_ID\",\"amount\":1000,\"method\":\"UPI\",\"couponCode\":\"NOPE\"}" || true

echo "== history =="
json GET "/api/users/$USER_ID/transactions"
json GET "/api/merchants/$MERCH_ID/transactions"
