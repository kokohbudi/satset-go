#!/usr/bin/env bash
# Keycloak config migrations — run same against dev & prod for identical KC state.
# Each scripts/kc-migrations/NNNN-*.sh is additive + idempotent (safe to re-run).
#
# Usage:
#   KC=http://localhost:9999 ADMIN=admin PASS=kozaninja ./scripts/kc-migrate.sh        # dev
#   KC=https://kc.prod       ADMIN=admin PASS=***       ./scripts/kc-migrate.sh        # prod
set -euo pipefail
KC="${KC:?set KC base url}"
ADMIN="${ADMIN:?set ADMIN}"
PASS="${PASS:?set PASS}"
export REALM="${REALM:-satset-go}"
export KC

export TOK=$(curl -fsS -X POST "$KC/realms/master/protocol/openid-connect/token" \
  -d client_id=admin-cli -d username="$ADMIN" -d password="$PASS" -d grant_type=password \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

echo "migrating KC=$KC realm=$REALM"
DIR="$(cd "$(dirname "$0")/kc-migrations" && pwd)"
for f in "$DIR"/[0-9]*.sh; do
  echo "- $(basename "$f")"
  bash "$f"
done
echo "done."
