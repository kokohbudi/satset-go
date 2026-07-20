#!/usr/bin/env bash
# Manually settle a stuck PROCESSING transaction as FAILED -> triggers refund.
#
# How it works: reuses our own webhook endpoint. We hold DIGIFLAZZ_WEBHOOK_SECRET,
# so we can sign a "Gagal" payload ourselves and POST it — same code path as a real
# Digiflazz callback, so it goes through TransactionDomainService.reconcileProviderResult
# (real refund, real WalletService audit trail), not a hand-rolled SQL update.
#
# rc=02 is used deliberately: NOT in DigiflazzStatusMapper's FORMS_TRANSACTION set
# ({01,50}), so it maps to FAILED (refund), not PENDING.
#
# ⚠️  VERIFY FIRST on Digiflazz's own dashboard/history that the transaction genuinely
#     failed / never formed before running this. If DF actually delivered the product,
#     this refund double-pays the customer (product + money back).
#
# Usage:
#   ./manual-refund.sh <ref_no> [webhook_url]
# (reads DIGIFLAZZ_WEBHOOK_SECRET from the environment, or falls back to .env at repo root)
#
# Idempotent: if the transaction is already terminal (SUCCESS/FAILED/REFUNDED),
# the app's own idempotency guard no-ops it — safe to re-run.

set -euo pipefail

REF_NO="${1:?usage: manual-refund.sh <ref_no> [webhook_url]}"
URL="${2:-https://satset-webhook.fly.dev/api/webhooks/digiflazz}"

if [ -z "${DIGIFLAZZ_WEBHOOK_SECRET:-}" ]; then
  ENV_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/.env"
  [ -f "$ENV_FILE" ] && DIGIFLAZZ_WEBHOOK_SECRET="$(grep '^DIGIFLAZZ_WEBHOOK_SECRET=' "$ENV_FILE" | cut -d= -f2-)"
fi
SECRET="${DIGIFLAZZ_WEBHOOK_SECRET:?set DIGIFLAZZ_WEBHOOK_SECRET (env var or repo .env)}"

BODY=$(printf '{"data":{"ref_id":"%s","customer_no":"manual-refund","buyer_sku_code":"manual","message":"Manual refund via ops script","status":"Gagal","rc":"02","sn":"","price":0}}' "$REF_NO")

SIG=$(printf '%s' "$BODY" | openssl dgst -sha1 -hmac "$SECRET" | sed 's/^.* //')

echo "POST $URL  ref_no=$REF_NO"
curl -sS -o /dev/stderr -w "\nHTTP %{http_code}\n" -X POST "$URL" \
  -H "Content-Type: application/json" \
  -H "X-Hub-Signature: sha1=$SIG" \
  -d "$BODY"
