# Reconcile — scheduled stuck-tx sweep (safety net)

**Date:** 2026-07-23
**Branch:** `feat/reconcile-sweep` (base: `main`)
**Status:** Approved design

## Problem

Topup transactions can stall in `PROCESSING` when Digiflazz returns a "Pending"
status: balance is already deducted, DF is still processing, and `reconcileProviderResult`
deliberately leaves the row `PROCESSING` to be settled later. The primary settler is
the standalone Digiflazz webhook (deployed to Fly.io, branch `feat/status-webhook`).
But the Fly app suspends and its egress IP rotates, so DF webhooks can be late or
never arrive. We need a **core-side safety net** that runs at the home/server IP
(stable, already whitelisted at DF) to sweep stalled rows and settle them.

This reinstates `TransactionReconcileService`, deleted 2026-07-18 (commit `2d254a9`).

## Scope

**In:** scan stale `PROCESSING` topup rows, re-POST to DF (idempotent by ref_id,
doubles as a status check), settle via the existing `reconcileProviderResult`.

**Out (explicitly):**
- `PENDING` rows — under the single `@Transactional` in `createPurchase`, status always
  advances to `PROCESSING` before any external call; a committed `PENDING` row is an
  anomaly whose balance-deduct state is unknown, so we do **not** re-POST it.
- Orphan tx lost at DF (row rolled back but DF charged) — needs DF-side mutasi
  reconcile, not a DB scan. Separate problem.

## Architecture

Core-only. No webhook-branch code touched. Files:

### 1. `TransactionReconcileService` (`transaction/service/reconcile/`)

Restore of the deleted service, plus `@LogContext("Reconcile")` (new — the service
predates the outbound-trace rule; routes business logs to `logs/Reconcile/`).

```
@Scheduled(fixedDelayString = "${topup.reconcile.interval-ms:60000}")
reconcileStalePending():
    now         = LocalDateTime.now()
    staleCutoff = now - stale-after-ms   // upper bound: row must be older than this
    maxCutoff   = now - max-age-ms        // lower bound: give-up past this
    // give-up ALERT, decoupled from the batch so stuck rows can't starve it (see §5)
    stuck = txRepo.countByStatusAndCreatedAtBefore(PROCESSING, maxCutoff)
    if stuck > 0: log ALERT ("{stuck} tx stuck > maxAge, manual Ops")
    // scan only the reconcilable window [maxCutoff, staleCutoff] — give-up rows excluded
    stale  = txRepo.findByStatusAndCreatedAtBetween(
                 PROCESSING, maxCutoff, staleCutoff,
                 PageRequest.of(0, batchSize, Sort.ASC "createdAt"))   // oldest first, capped
    if empty: return
    for row in stale:
        try: transactionTemplate.executeWithoutResult(_ -> settleOne(row))   // per-row tx
        catch: log.error, leave PROCESSING (retried next run) — batch not poisoned

settleOne(row):
    tx = txRepo.findById(row.id)
    if tx == null || tx.status != PROCESSING: return          // guard: webhook already settled
    denom = denomRepo.findDenomInfoById(tx.productDenomId)
    if denom == null: log.warn, return
    resp = provider.sendTransaction(tx.targetNumber, denom.code(), tx.total, refIdFor(tx))
    txService.reconcileProviderResult(tx, resp, tx.walletId, denom)

refIdFor(tx): tx.refNo != null ? tx.refNo : tx.id.toString()   // pre-ref_no rows fall back to UUID
```

**Not `@Transactional`:** each row settles in its own `transactionTemplate` transaction
so one failing row can't mark a shared tx rollback-only and poison the batch (a bare
per-row try/catch inside one `@Transactional` does NOT protect — Spring still throws
`UnexpectedRollbackException` at commit).

**Provider seam:** `ProviderPort.sendTransaction` → `RealProviderAdapter` →
`DigiflazzClient.topup`. Deliberate: keeps the mockable seam the rest of the flow uses,
even though the task note named `DigiflazzClient` directly.

### 2. Repo finder (re-add to `TransactionRepository`)

```java
List<Transactions> findByStatusAndCreatedAtBefore(
        TransactionStatus status, LocalDateTime cutoff, Pageable pageable);
```

### 3. `@EnableScheduling`

Removed when the service was deleted. Re-add to a core config/main class.

### 4. Config (`application.yml`)

```
topup:
  reconcile:
    interval-ms: 60000       # @Scheduled fixedDelay
    stale-after-ms: 120000   # row must be older than this to be swept
    batch-size: 100          # max rows per run (rate-limit guard vs DF rc 85)
    max-age-ms: 21600000     # 6h — stop re-polling past this, alert Ops instead
```

### 5. Give-up cutoff (A + B)

A row keeps being re-polled every run while DF stays "Pending" (A). But past
`max-age-ms` (6h) it is a stuck row DF will likely never resolve. Rather than
re-poll it forever, the scan window is **two-bounded** — `[maxCutoff, staleCutoff]`
— so give-up rows (older than `maxCutoff`) fall *out* of the reconcile batch. This
also prevents a backlog of permanently-stuck rows from starving newer, still-
reconcilable ones (they'd otherwise fill the oldest-first, batch-capped scan).

Ops visibility (B) is preserved but **decoupled** from the batch: once per sweep a
separate `countByStatusAndCreatedAtBefore(PROCESSING, maxCutoff)` counts the give-up
rows and, if any, logs one aggregate `ALERT` line to `logs/Reconcile/`. Those rows
stay `PROCESSING` (never auto-FAILED/refunded — DF might still settle them, and
flipping without a DF verdict risks a wrong refund); Ops resolves them manually.

```
stuck = txRepo.countByStatusAndCreatedAtBefore(PROCESSING, maxCutoff)
if stuck > 0: log.error("ALERT: {} tx stuck PROCESSING > maxAge, need manual Ops", stuck)
```

ponytail: the aggregate ALERT re-fires every sweep while give-up rows exist — noisy
by design (an alert that keeps firing until Ops clears it). Config invariant
`stale-after-ms < max-age-ms` must hold or the scan window inverts; the defaults
satisfy it. Follow-up: an `ArgumentCaptor` test pinning the bound order, and a real-
Postgres concurrent-settle test for the `@Version` layer.

## Anti-double-settle (webhook + scheduler)

No new code — three existing layers cover it:

1. **Scan is `PROCESSING`-only** — once the webhook flips a row to SUCCESS/REFUNDED,
   the finder no longer returns it.
2. **Re-fetch + status guard** in `settleOne` (`status != PROCESSING → return`) — covers
   the sequential case (webhook settled seconds ago).
3. **`@Version` optimistic lock** on `Transactions` — covers a true concurrent race.
   Both settlers load version N; the winner commits (N→N+1). The loser's flush issues
   `UPDATE … WHERE version = N` → 0 rows → `OptimisticLockException`, rolling back its whole
   per-row `TransactionTemplate` transaction — including any `refundBalance` it performed
   (wallet is the same datasource, so the refund reverts atomically with it). Zero double refund.
   (Safety rests on transactional atomicity, not on precise `save()` flush timing.)

**Requirement on the webhook (its own branch, not this one):** the webhook MUST settle via
the same `reconcileProviderResult` so it hits the same `@Version` row. Already true on
`feat/status-webhook`.

## Testing (TDD, red→green)

**Service — Mockito:**
- stale `PROCESSING` found → `provider.sendTransaction` + `reconcileProviderResult` called with right args
- re-fetch guard: row flipped to SUCCESS between scan and settle → skipped, no provider call
- empty scan → no-op
- per-row error isolation: row A throws → row B still settled
- batch cap: `PageRequest` size == configured `batch-size`
- refId fallback: null `refNo` → UUID string used
- give-up: rows older than `max-age-ms` are excluded from the scan window; a separate count query drives one aggregate ALERT, and those rows are never re-polled

**Repo — `@DataJpaTest`:**
- finder returns only `PROCESSING` older than cutoff, ordered `createdAt` ASC, limited by `Pageable`

## Out of scope / follow-ups

- Metrics/alerting on reconcile volume — later if backlog grows.
- DF-side orphan reconcile (lost tx) — separate task.
