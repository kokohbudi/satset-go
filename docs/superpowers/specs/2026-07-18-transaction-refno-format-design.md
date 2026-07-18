# Transaction ref_no: date + daily counter (YYYYMMDDXXXXX)

**Date**: 2026-07-18
**Branch**: feat/digiflazz-topup
**Status**: Approved design

## Problem

The Digiflazz `ref_id` (idempotency key for `/transaction`) is currently the
transaction UUID PK stringified — `transaction.getId().toString()` — set in two
call sites:

- `TransactionDomainService.java:102` (purchase create)
- `TransactionReconcileService.java:94` (reconcile re-poll)

Two issues:

1. **Opaque for CS/support** — a 36-char UUID is impossible to read out over the
   phone or eyeball in a ledger.
2. **Length** — 36 chars risks Digiflazz `ref_id` length limits.

## Goal

Replace the outbound `ref_id` with a human-readable `YYYYMMDDXXXXX`:

- `YYYYMMDD` — transaction date, **Asia/Jakarta (WIB)** bucket.
- `XXXXX` — 5-digit zero-padded **daily** counter, resets to `00001` each WIB day.
- Capacity: 99,999 transactions/day. (Widen the counter if volume approaches this.)

The value is generated **once** at transaction creation, **persisted** on the
transaction, and reused verbatim on reconcile — this is what keeps Digiflazz
idempotency intact (same `ref_id` re-POST = status check, never a re-charge).

The UUID PK is unchanged — it stays the internal primary key. `ref_no` becomes
the user/CS-facing invoice number shown in the UI.

## Non-goals

- Backfilling old transactions. `ref_no` is nullable; pre-existing rows stay
  null and the UI falls back to the UUID for them.
- Changing the wallet mutation `referenceId` (still the transaction UUID).

## Design

### 1. Atomic daily counter — `ref_counter` table + UPSERT RETURNING

Concurrent transactions must never receive the same counter value — a collision
would produce a duplicate Digiflazz `ref_id` and corrupt idempotency (potential
double-charge). A read-then-increment (`COUNT(*)+1`) is race-prone and rejected.
A Postgres SEQUENCE is atomic but not date-scoped and awkward to reset daily.

Chosen: a counter table with an atomic UPSERT:

```sql
INSERT INTO ref_counter(day, seq) VALUES (:day, 1)
ON CONFLICT (day) DO UPDATE SET seq = ref_counter.seq + 1
RETURNING seq;
```

Single statement, race-safe under Postgres row locking, and the daily reset is
natural — a new `day` row starts at 1. Gaps (when an outer transaction rolls
back after the counter advanced) are acceptable: a gap is harmless, a collision
is not.

New JPA entity `RefCounter` exists so Hibernate `ddl-auto` creates the table;
the increment itself bypasses JPA dirty-checking via the native UPSERT.

- `RefCounter`: `day DATE @Id`, `seq BIGINT`.
- **Must be registered in `CoreDataSourceConfig` (both entity + repository
  lists)** — a new `@Entity` module missing from either list fails the whole
  context load (only `@SpringBootTest` catches it).

### 2. `RefNoGenerator` service

- `LocalDate.now(ZoneId.of("Asia/Jakarta"))` → day bucket.
- Native UPSERT (above) via the counter repository → `seq`.
- Format: `day.format(BASIC_ISO_DATE)` (`YYYYMMDD`) + `String.format("%05d", seq)`.
- Internal DB access, not an outbound supplier call → **no `@LogContext`**
  (the outbound-must-go-through-traced-service rule does not apply to own-DB access).

### 3. `Transactions` entity

Add:

```java
@Column(name = "ref_no", unique = true, length = 20)
private String refNo;
```

Nullable (old rows), unique (catches generator bugs early).

### 4. Wire into purchase + reconcile

- `TransactionDomainService`: after the transaction is saved (PK exists) and
  before the provider call, generate `refNo`, set it, save. Line 102 sends
  `transaction.getRefNo()` instead of `transaction.getId().toString()`.
- `TransactionReconcileService.settleOne` (line 94): send `tx.getRefNo()`.
  Reads the persisted value — no regeneration — so the re-POST carries the
  identical `ref_id`.

### 5. UI / CS display

- The transaction response DTO exposes `refNo`.
- History/detail templates show `refNo`, falling back to the UUID when `refNo`
  is null (old transactions).

## Testing (TDD — red first)

- `RefNoGenerator`: format `YYYYMMDDXXXXX`, zero-padding (`00001`), daily reset
  (new day → back to `00001`).
- Counter uniqueness under concurrency: N parallel generates on the same day
  yield N distinct sequential values, no duplicates.
- Reconcile idempotency: a PROCESSING transaction re-polled sends the **same**
  `ref_no` it was created with (not a fresh value).
- UI fallback: transaction with null `refNo` renders the UUID.

## Rollout

- Dev: `ddl-auto=update` creates `ref_counter` + `transactions.ref_no`.
- Prod: `ddl-auto=validate` — needs manual DDL: `CREATE TABLE ref_counter`,
  `ALTER TABLE transactions ADD COLUMN ref_no VARCHAR(20) UNIQUE`.
