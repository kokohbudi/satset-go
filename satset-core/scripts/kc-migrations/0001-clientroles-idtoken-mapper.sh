#!/usr/bin/env bash
# 0001: add ID-token "client roles" mapper to satsetgo-client (additive, admin-safe, idempotent).
# Consumes from runner: KC, TOK, REALM.
set -euo pipefail
: "${KC:?}" "${TOK:?}" "${REALM:?}"
CLIENT="${TARGET_CLIENT:-satsetgo-client}"

CID=$(curl -fsS "$KC/admin/realms/$REALM/clients?clientId=$CLIENT" \
  -H "Authorization: Bearer $TOK" | python3 -c "import sys,json;print(json.load(sys.stdin)[0]['id'])")

EXISTS=$(curl -fsS "$KC/admin/realms/$REALM/clients/$CID/protocol-mappers/models" \
  -H "Authorization: Bearer $TOK" \
  | python3 -c "import sys,json;print(any(m['protocolMapper']=='oidc-usermodel-client-role-mapper' for m in json.load(sys.stdin)))")
if [ "$EXISTS" = "True" ]; then echo "  0001 skip (client-roles mapper present)"; exit 0; fi

HTTP=$(curl -fsS -o /tmp/kc_resp -w "%{http_code}" -X POST \
  "$KC/admin/realms/$REALM/clients/$CID/protocol-mappers/models" \
  -H "Authorization: Bearer $TOK" -H "Content-Type: application/json" -d '{
  "name": "client roles",
  "protocol": "openid-connect",
  "protocolMapper": "oidc-usermodel-client-role-mapper",
  "config": {
    "claim.name": "resource_access.${client_id}.roles",
    "jsonType.label": "String",
    "multivalued": "true",
    "id.token.claim": "true",
    "access.token.claim": "true",
    "introspection.token.claim": "true",
    "userinfo.token.claim": "false"
  }
}')
[ "$HTTP" = "201" ] && echo "  0001 applied (mapper created)" || { echo "  0001 FAILED http=$HTTP"; cat /tmp/kc_resp; exit 1; }
