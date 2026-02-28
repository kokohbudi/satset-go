# Balance & Ledger Architecture Design

## Overview
To prevent race conditions and ensure 100% auditability for store balances, the system uses a **Double-Entry Ledger** (buku tabungan) architecture combined with a cached running balance on the Stores entity.

## Architecture & Components

### Source of Truth: `StoreMutations`
Every financial movement creates an **immutable** ledger entry:
- `id`: UUID
- `store`: Reference to `Stores`
- `amount`: Absolute value of the transaction (`BigDecimal`)
- `type`: `CREDIT` (top-up, refund) or `DEBIT` (purchase)
- `balanceAfter`: Running balance *after* this mutation is applied
- `referenceType`: Polymorphic type (`TOP_UP`, `PURCHASE`, `REFUND`, `ADJUSTMENT`)
- `referenceId`: UUID link to the source document (e.g., Transaction ID, Top-up ID)
- `description`: Human-readable context
- `createdAt`: Audit timestamp (immutable — no `updatedAt`)

### Cache: `Stores.balance`
Acts as a high-performance read-cache (snapshot), synced atomically during every mutation.

### Polymorphic References
Instead of a nullable FK to `Transactions`, the ledger uses `referenceType + referenceId` — enabling future extension (promo, admin adjustment) without schema changes.

## Data Flow & Concurrency (Write Flow)
To ensure `balanceAfter` is strictly sequential and accurate:
1. Open `@Transactional` boundary.
2. Acquire Pessimistic Write Lock on `Stores` (`SELECT ... FOR UPDATE`).
3. Read current balance from `Stores.balance` cache.
4. Validate sufficient balance (for `DEBIT`).
5. Calculate new balance: `currentBalance +/- amount`.
6. Insert into `StoreMutations` with `balanceAfter = newBalance`.
7. Update `Stores.balance` with `newBalance` (sync cache).
8. Commit transaction (releases the database row lock).

## Error Handling & Reconciliation
- **Race Conditions**: Prevented by pessimistic lock on `Stores` row as serialization point.
- **Data Desync / Self-Healing**: If `Stores.balance` drifts, recalculate from `StoreMutations.balanceAfter` of latest entry.
- **Insufficient Balance**: Throws `InsufficientBalanceException` (HTTP 400).

## Implementation Status: ✅ DONE (2026-02-24)
- `BalanceService.java` — deductBalance, addBalance, getBalance
- `StoreMutations.java` — immutable ledger entity
- `StoreMutationRepository.java` — findTopByStoreOrderByCreatedAtDesc
