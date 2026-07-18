# Digiflazz Topup Integration — Design

**Date**: 2026-07-18
**Status**: Approved (design), pending spec review
**Scope**: Full slice — real prepaid topup + async-safe status handling + reconcile poll

---

## Goal

Wire the real Digiflazz prepaid topup (`POST /v1/transaction`) into the purchase
flow, replacing the `RealProviderAdapter` stub, and handle Digiflazz's
asynchronous **Pending** status without leaking money (no refund-then-success
double-spend). Settle Pending transactions via an **outbound poll** job — no
public endpoint required.

## Non-goals (deliberately deferred)

- **Inbound webhook** (`cb_url`). Feasible later (DF POSTs `data{}` with
  `X-Hub-Signature: sha1=HMAC-SHA1(body, secret)`), but needs a public endpoint
  + secret. Outbound poll covers settlement without it. Add when poll volume hurts.
- **Egress static-IP whitelist**. DF only accepts requests from IPs registered in
  its Connection Settings; the old Fly proxy (static egress) was deleted
  2026-07-15. `supplier.mode=real` will NOT reach DF in prod until a whitelisted
  egress exists. This is infra, out of scope here — build/test against mock +
  `MockRestServiceServer`.
- **Pre-flight saldo check** (`/v1/deposit`). YAGNI — `rc 44` (DF balance low) is
  handled as a topup failure.

---

## Digiflazz contract (reference)

**Topup** — `POST https://api.digiflazz.com/v1/transaction`
```json
{ "username": "...", "buyer_sku_code": "xld25", "customer_no": "0878...",
  "ref_id": "<txId>", "sign": "md5(username+apiKey+ref_id)" }
```
Response wrapped in `data{}`: `ref_id, customer_no, buyer_sku_code, message,
status, rc, sn, buyer_last_saldo, price, tele, wa`.

**Prepaid status check** = re-POST the SAME `/v1/transaction` with the same
`ref_id`. Idempotent, does not re-charge; returns current status. (The
`status-pasca` endpoint is postpaid-only — not used here.)

**Status/rc mapping** (money-safe: unknown → PENDING, never auto-refund):

| DF `status` | `rc`            | → ProviderStatus | createPurchase action        |
|-------------|-----------------|------------------|------------------------------|
| `Sukses`    | `00`            | `SUCCESS`        | mark SUCCESS, set sn/cost    |
| `Pending`   | `03`            | `PENDING`        | leave PROCESSING, no refund  |
| `Gagal`     | `01`,`50`       | `PENDING`        | leave PROCESSING (tx may have formed — poll) |
| `Gagal`     | other           | `FAILED`         | refund                       |
| anything else / parse fail | any  | `PENDING`        | leave PROCESSING (poll resolves) |

`price` in the response = our supplier cost → `ProviderResponse.cost` (margin).

---

## Changes

Existing flow (`TransactionDomainService.createPurchase`): PENDING → deduct
balance → PROCESSING → `providerService.sendTransaction(...)` → binary
SUCCESS / FAILED+refund. We make it three-state and reuse the settle logic for
the poll job.

### 1. `ProviderStatus` enum (transaction/model)
```java
public enum ProviderStatus { SUCCESS, PENDING, FAILED }
```

### 2. `ProviderResponse` — carry status instead of a bare boolean
```java
public record ProviderResponse(
    ProviderStatus status, String referenceNumber, String serialNumber,
    String message, BigDecimal cost) {
    public boolean success() { return status == ProviderStatus.SUCCESS; }
}
```
`success()` kept as a derived helper so existing call sites read naturally.

### 3. `ProviderPort` — pass the reference id
```java
ProviderResponse sendTransaction(String targetNumber, String denomCode,
                                 BigDecimal amount, String refId);
```
`refId` = the transaction UUID string. It becomes DF's `ref_id` (idempotency +
status re-query key). Update `MockProviderAdapter` (ignore refId, return
`SUCCESS`/`FAILED` as today) and the `createPurchase` call site.

### 4. `DigiflazzClient.topup(refId, buyerSkuCode, customerNo)` (supplier slice)
Owns the DF protocol: builds the request, signs `md5(username+apiKey+refId)`
(reuse the existing private `sign(...)`), POSTs `/transaction`, parses `data{}`,
and returns a supplier-local result record:
```java
public record DigiTxResult(String status, String rc, String refId,
                           String sn, BigDecimal price, String message) {}
```
Parse errors / `data` object being an error shape → return a result mapped to
PENDING by the adapter (money-safe), logging `rc`+`message`. No transaction
types leak into the supplier slice.

### 5. `RealProviderAdapter` — thin map DF → port
Replace the stub: call `digiflazzClient.topup(refId, denomCode, targetNumber)`,
apply the status/rc mapping table above, return `ProviderResponse`. Inject
`DigiflazzClient` (drop the raw `RestClient` field).

### 5b. `Transactions.walletId` — persist the charged wallet
Add `@Column(name = "wallet_id", length = 50) private String walletId;`
(nullable — legacy rows have none). Set `transaction.setWalletId(walletId)` in
`createPurchase` so the reconcile job can refund without request context.
DDL-auto `update` adds the column in dev; prod needs `ALTER TABLE`.

### 6. `createPurchase` — three-state settle, extracted for reuse
After `sendTransaction`, replace the `if (success) … else …` with a call to a
new private/package method:
```java
void reconcileProviderResult(Transactions tx, ProviderResponse r,
                         String walletId, DenomInfo denom);
```
- `SUCCESS` → set SUCCESS, providerRef, sn, costPrice, margin (as today).
- `PENDING` → keep PROCESSING, set providerRef if present, no refund. Log.
- `FAILED` → refund path (as today).

### 7. Reconcile poll — `TransactionReconcileService`
```java
@Scheduled(fixedDelayString = "${supplier.reconcile.interval-ms:60000}")
void reconcileStalePending() { ... }
```
- Repo: `List<Transactions> findByStatusAndCreatedAtBefore(TransactionStatus,
  LocalDateTime)` — PROCESSING older than `${supplier.reconcile.stale-after-ms:120000}`
  (>1 min, DF's own re-query throttle).
- For each: re-call `providerService.sendTransaction(target, code, total,
  tx.getId().toString())` (same refId → DF returns current status) →
  `reconcileProviderResult`. Needs `walletId`/`denom` for the refund branch:
  `walletId` is read from a new `transactions.wallet_id` column (set at
  `createPurchase` time — `userDTO.getWalletId()` is request-scoped and absent
  in a scheduled job, so the charged wallet must be persisted on the row);
  `denom` via `denomRepository.findDenomInfoById(tx.getProductDenomId())`.
- Idempotent: SUCCESS/FAILED/REFUNDED rows are never selected again.
- `ponytail:` batch cap per run (e.g. 100) so a backlog can't stampede DF's
  rate limit (rc 85); widen if throughput needs it.

### 8. Config (`application.yml`)
```yaml
supplier:
  mode: mock            # real in prod (needs whitelisted egress)
  reconcile:
    interval-ms: 60000
    stale-after-ms: 120000
  digiflazz:
    base-url: https://api.digiflazz.com/v1
    username: ${DIGIFLAZZ_USERNAME:}
    api-key: ${DIGIFLAZZ_API_KEY:}
```
`@EnableScheduling` if not already on.

---

## Testing (TDD, red→green)

1. **`DigiflazzClient.topup`** (`MockRestServiceServer`): asserts request body
   (`buyer_sku_code`, `customer_no`, `ref_id`, correct `sign`), parses each
   `data{}` shape → Sukses / Pending / Gagal / error-object / malformed.
2. **`RealProviderAdapter`**: status/rc mapping table incl. money-safe edges
   (`Gagal`+`rc 01` → PENDING; `Gagal`+`rc 44` → FAILED; parse fail → PENDING).
3. **`createPurchase` PENDING path**: tx stays PROCESSING, wallet NOT refunded,
   providerRef stored. (SUCCESS/FAILED paths already covered.)
4. **`TransactionReconcileService`**: PROCESSING+stale re-polled → Sukses settles
   to SUCCESS; Gagal(terminal) refunds; still-Pending stays PROCESSING;
   fresh/non-PROCESSING rows skipped.

---

## Risks

- **Egress IP** (above) blocks real prod until infra exists — mock stays default.
- **Refund-on-reconcile-failure**: existing behavior kept — if wallet refund
  throws, row stays FAILED for manual Ops refund (logged `ALERT`).
- **DF rate limit (rc 85)** on poll bursts → batch cap + `stale-after >1min`.
