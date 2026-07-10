#!/bin/bash
# Digiflazz smoke-test via proxy Fly (egress 209.71.95.98). Semua call lewat proxy.
# Env: DF_USER, DF_KEY (dari .env: DIGIFLAZZ_USERNAME/DIGIFLAZZ_DEV_KEY),
#      PROXY_PASS (dari `fly secrets` app satset-wg-gateway).
#
#   DF_USER=xx DF_KEY=yy PROXY_PASS=zz ./df.sh saldo
#   ... ./df.sh pricelist
#   ... ./df.sh topup <SKU> <customer_no> <ref_id>   # testing:true; 087800001230=Sukses
set -euo pipefail
: "${DF_USER:?set DF_USER}"; : "${DF_KEY:?set DF_KEY}"; : "${PROXY_PASS:?set PROXY_PASS}"
PROXY="http://satset:${PROXY_PASS}@137.66.22.249:8888"
API="https://api.digiflazz.com/v1"

md5hex() { printf '%s' "$1" | { md5 -q 2>/dev/null || md5sum | cut -d' ' -f1; }; }
post() { curl -sS -x "$PROXY" -X POST "$API/$1" -H "Content-Type: application/json" -d "$2"; echo; }

case "${1:-}" in
  saldo)     post cek-saldo  "{\"cmd\":\"deposit\",\"username\":\"$DF_USER\",\"sign\":\"$(md5hex "${DF_USER}${DF_KEY}depo")\"}" ;;
  pricelist) post price-list "{\"cmd\":\"prepaid\",\"username\":\"$DF_USER\",\"sign\":\"$(md5hex "${DF_USER}${DF_KEY}pricelist")\"}" ;;
  topup)
    SKU="${2:?SKU}"; CUST="${3:?customer_no}"; REF="${4:?ref_id}"
    post transaction "{\"username\":\"$DF_USER\",\"buyer_sku_code\":\"$SKU\",\"customer_no\":\"$CUST\",\"ref_id\":\"$REF\",\"testing\":true,\"sign\":\"$(md5hex "${DF_USER}${DF_KEY}${REF}")\"}" ;;
  *) echo "usage: ./df.sh saldo|pricelist|topup <SKU> <customer_no> <ref_id>"; exit 1 ;;
esac
