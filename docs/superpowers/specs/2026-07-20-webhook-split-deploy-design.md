# Digiflazz Webhook — Split Deploy (Fly) → Merge into Core — Design

**Date**: 2026-07-20
**Status**: Approved (design), pending spec review
**Scope**: WH-series (Tasks.md) — inbound webhook receiver, deployed standalone now, folded into `satset-core` at prod launch

---

## Goal

Replace the deleted polling reconciler (`TransactionReconcileService`, `@Scheduled`
60s, removed 2026-07-18) with a real Digiflazz callback. `satset-core` currently
only runs on a local dev machine (no public IP) and isn't deploying to prod yet;
Digiflazz needs a public HTTPS endpoint *now*, and delivery must settle
**instantly** even when the dev machine is off. So the webhook receiver ships as
its own small deployable (`satset-webhook`, on Fly.io) today, reusing core's
settlement logic directly via a Maven dependency — no HTTP/token-exchange back
to core, no duplicated business logic. When core itself deploys to prod, the
receiver folds back in as one deploy (see Migration).

## Non-goals (deliberately deferred)

- **Egress whitelist / `supplier.mode=real`.** Separate problem (outbound IP
  whitelisting for the topup call itself); old Fly proxy for that was already
  torn down 2026-07-15 (see `2026-07-18-digiflazz-topup-design.md`). Not
  addressed here — this design is inbound-only.
- **Seller/Pascabayar (postpaid) webhooks.** `RealProviderAdapter` only does
  prepaid buyer topup today; postpaid webhook contract is a different DF doc,
  out of scope until postpaid exists.
- **Refund-failure stuck-`FAILED` edge case** (`reconcileProviderResult`
  lines 150-164: if the wallet refund call throws, the row stays `FAILED` for
  manual ops). Pre-existing gap in the domain service, unrelated to the
  webhook itself — not fixed here.
- **Webhook secret rotation / dashboard automation.** Secret is a single env
  var (Fly secret / `.env`), provisioned manually via Digiflazz's dashboard
  ("Atur Koneksi > API > Webhook"). No admin UI for it.

---

## Digiflazz webhook contract (reference — WH-0)

Source: `developer.digiflazz.com/api/buyer/webhook/`.

- **Registration**: manual, via Digiflazz's own dashboard (not an API call) —
  "Atur Koneksi > API > Webhook". Generates the callback URL + shared secret.
- **Delivery**: `POST`, `Content-Type: application/json`. Headers:
  - `X-Digiflazz-Event`: `create` / `update` / `resend`
  - `X-Hub-Signature`: `sha1=<hex>` — HMAC-SHA1 over the **raw request body**
    using the shared secret. PHP reference: `hash_hmac('sha1', $post_data, $secret)`.
  - `User-Agent: Digiflazz-Hookshot` (prepaid)
- **Payload** (prepaid), wrapped in `data{}`:
  ```json
  { "data": { "ref_id": "30467470", "customer_no": "081280556115",
    "buyer_sku_code": "ovo100", "message": "Sukses", "status": "Sukses",
    "rc": "00", "buyer_last_saldo": 326719460,
    "sn": "SEPTIAPAR/20190401214753214742", "price": 199800,
    "tele": "@telegram", "wa": "081234512345" } }
  ```
  `status`/`rc` values are the same vocabulary already handled by
  `RealProviderAdapter.mapStatus` (`Sukses`/`Gagal`/rc table — see
  `2026-07-18-digiflazz-topup-design.md`).
- **Retry/redelivery behavior and expected response contract: not
  documented.** Design defensively — always respond fast, treat any non-2xx
  from us as "they may retry," and make the handler idempotent regardless.
- Ping/connectivity test: `POST api.digiflazz.com/v1/report/hooks/[ID]/pings`.

---

## Architecture

New Maven module `satset-webhook` (root `pom.xml` gets a second `<module>`
entry — precedented: `satset-wallet` was hosted this way 2026-03-07 through
2026-06-22, and the parent POM's shared `dependencyManagement`/properties/
build-plugins already apply to any child for free).

- Depends on `satset-core` as a compile dependency — reuses
  `TransactionDomainService.reconcileProviderResult`, `Transactions`/
  `TransactionRepository`, `ProviderResponse`/`ProviderStatus`, and the
  status/rc mapper (extracted, see Changes #1).
- Own `@SpringBootApplication`, own `application.yml` pointing at the same
  Neon DB (already shared — no new datasource).
- Own minimal `SecurityConfig`: **no Keycloak/OAuth2 at all** — this app
  serves exactly one public endpoint. Permit `/api/webhooks/digiflazz`,
  deny everything else. Smaller surface than core, and drops the Keycloak
  Testcontainer dependency for this module's tests (Postgres only).
- Deployed standalone to Fly.io. Cost precedent: `fly-proxy/` ran a small
  container there for ~$6/mo (2026-07-10 to 2026-07-18, different purpose).

## Changes

### 1. Extract status/rc mapping (satset-core, `transaction/client`)
`RealProviderAdapter.mapStatus(status, rc)` becomes a shared, package-visible
static mapper (e.g. `DigiflazzStatusMapper.map(status, rc)` next to
`RealProviderAdapter`) so both the synchronous topup path and the new webhook
payload mapper apply the exact same table — no second copy of the
Sukses/Gagal/rc-01/rc-50 logic to drift out of sync.

### 2. `TransactionRepository.findByRefNo(String refNo)` (satset-core)
Missing today — needed to look up the transaction from Digiflazz's `ref_id`
(== our `refNo`).

### 3. Idempotency guard in `reconcileProviderResult` (satset-core)
Today it unconditionally re-applies whatever transition the response implies.
Add an early return: if `transaction.getStatus()` is already `SUCCESS`,
`FAILED`, or `REFUNDED`, no-op. This is what makes Digiflazz's `resend`
redelivery (and any duplicate `create`/`update`) safe — and it's a real
correctness fix regardless of the split (WH-4).

### 4. `satset-webhook` module — new files
- `DigiflazzWebhookController` — `POST /api/webhooks/digiflazz`. Binds the
  body as `@RequestBody String rawBody` (not a parsed DTO) specifically so
  the signature check runs over the exact bytes Digiflazz signed, then
  parses JSON manually via `ObjectMapper` after verification passes.
- `DigiflazzSignatureVerifier` — HMAC-SHA1(`rawBody`, secret), compares
  against `X-Hub-Signature: sha1=<hex>` (constant-time compare).
- Payload → `ProviderResponse` mapping, calling `DigiflazzStatusMapper` (#1).
- `application.yml`: `digiflazz.webhook.secret: ${DIGIFLAZZ_WEBHOOK_SECRET:}`.

## Data flow

```
Digiflazz → POST /api/webhooks/digiflazz (Fly, satset-webhook)
  → DigiflazzSignatureVerifier.verify(rawBody, X-Hub-Signature)  [fail → 401, log, stop]
  → parse rawBody → extract ref_id
  → TransactionRepository.findByRefNo(ref_id)                    [not found → 404]
  → guard: status in {SUCCESS, FAILED, REFUNDED} → no-op, 200 (idempotent replay)
  → map payload → ProviderResponse via DigiflazzStatusMapper
  → TransactionDomainService.reconcileProviderResult(tx, response, walletId, denom)
  → 200
```

## Error handling

- Invalid signature → `401`, logged via `@LogContext("Webhook")` (WH-5), no DB touch.
- Unknown `ref_id` → `404` (not `500` — likely bad/forged payload, not our fault).
- Already-terminal transaction (replay) → `200`, no-op.
- Malformed JSON → `400`.
- `reconcileProviderResult` throws (DB down, etc.) → `500`, let Digiflazz's
  (undocumented) retry behavior handle it — safe because of the idempotency
  guard (#3).

## Testing (TDD, red→green)

1. `DigiflazzStatusMapper` — table-driven, same cases as the topup design's
   status/rc table (reused, not re-derived).
2. `DigiflazzSignatureVerifier` — valid HMAC accepts, tampered body/wrong
   secret rejects.
3. `reconcileProviderResult` idempotency guard — already-`SUCCESS`/`FAILED`/
   `REFUNDED` row + any incoming response → no state change, no double refund.
4. Integration (Testcontainers Postgres, no Keycloak needed): full POST →
   signature ok → PENDING/SUCCESS/FAILED payload settles correctly; invalid
   signature → 401 + no DB write; replay of an already-settled `ref_id` →
   200 no-op; unknown `ref_id` → 404.

## Migration to prod (merge)

1. Move `DigiflazzWebhookController` + `DigiflazzSignatureVerifier` +
   `DigiflazzStatusMapper` (if not already in core from #1) into a `webhook/`
   feature slice inside `satset-core` (web/service/model per CLAUDE.md's
   vertical-slice pattern).
2. Delete the `satset-webhook` module + its `<module>` line in root `pom.xml`.
3. Tear down the `satset-webhook` Fly app; the webhook path now serves from
   core's own deploy.
4. No data migration — same Neon DB throughout the whole lifecycle.
5. `findByRefNo` + the idempotency guard (#2, #3) stay in core permanently —
   correctness fixes, not split-only scaffolding.

## Risks

- **No documented DF retry contract** — mitigated by always being idempotent
  (#3) rather than relying on any particular DF retry/backoff behavior.
- **Raw-body signature verification must run before/around Spring Security's
  own body handling** — confirm `@RequestBody String` binding doesn't get
  double-consumed by any filter in the (minimal) security chain for this app;
  covered by test #4.
- **Webhook secret provisioning** — Fly secret for `satset-webhook`, `.env`
  for local testing (via the ping endpoint); never committed.
