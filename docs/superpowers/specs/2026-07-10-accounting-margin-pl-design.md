# Accounting Backoffice — Slice 0+1: Cost Snapshot + Margin/P&L

**Date**: 2026-07-10
**Status**: Approved (design), pending implementation plan
**Scope**: Slice 0 (cost snapshot) + Slice 1 (margin/P&L reports)

## Context

Backoffice accounting for the PPOB marketplace. The full landscape was scoped into
four dependency-ordered slices:

- **Slice 0** — cost snapshot on each transaction *(this spec)*
- **Slice 1** — margin / P&L reports *(this spec)*
- Slice 2 — reconciliation (wallet integrity, bank, provider) — deferred, next
- Slice 3 — general ledger (double-entry) — deferred until audit/tax demands it
- Slice 4 — tax/compliance — deferred

`WalletMutationEntity` already provides a single-entry ledger; it holds the line
until Slice 3 is genuinely forced.

## Money flow (worked example)

Supplier charges **6000** for IM3 5000. Reseller pays **6500**. Margin = **500**.

- Reseller pays `total` (= `price` + `adminFee`) → 6500
- Platform pays provider `costPrice` (modal) → 6000
- **Margin = total − costPrice** → 500

Margin is only meaningful for `SUCCESS` transactions. `FAILED`/`REFUNDED` → no cost,
no margin.

## Current state (gap)

`Transactions` snapshots `price`, `adminFee`, `total` (sell side ✓) but **no cost**,
so historical margin is uncomputable. `DenomInfo` (shared-kernel record used by the
purchase flow) exposes `price`/`adminFee` but not `basePrice`. `ProviderResponse` has
no cost field.

The purchase flow lives in `TransactionDomainService.createPurchase` and uses
`DenomInfo` (via `DenomRepository.findDenomInfoById`), **not** the `ProductDenoms`
entity directly.

## Slice 0 — Cost snapshot

Cost source is **both**: snapshot `denom.basePrice` as default, override with the
provider's actual cost when the provider returns one.

```
costPrice = response.cost() != null ? response.cost() : denom.basePrice()
margin    = total.subtract(costPrice)
```

Changes:

1. **`DenomInfo`** (record, `shared/model`): add `BigDecimal basePrice`. Update the
   projection query `DenomRepository.findDenomInfoById` to select it.
2. **`ProviderResponse`** (record, `transaction/model`): add `BigDecimal cost`
   (nullable — mock/real providers that don't report cost pass `null`).
3. **`Transactions`** (entity): add columns `costPrice` (BigDecimal 15,2, nullable)
   and `margin` (BigDecimal 15,2, nullable). DDL auto=`update` in dev adds them; prod
   `validate` needs the columns present first.
4. **`TransactionDomainService.createPurchase`**: at the SUCCESS branch (step 5a),
   compute and set `costPrice` + `margin` before saving. Leave both `null` on
   FAILED/REFUNDED.
5. **`MockProviderAdapter`**: return `cost = null` so the basePrice fallback path is
   exercised end-to-end.

Edge cases:
- `basePrice` null in catalog → `costPrice` null → margin null (report treats as
  "cost unknown", excluded from margin totals, surfaced as a data-quality flag).
- Only `SUCCESS` rows carry cost/margin; all reports filter `status = SUCCESS`.

## Slice 1 — Margin / P&L reports

1. **Aggregate queries** (JPQL `@Query`, `GROUP BY`, date-range params, `status =
   SUCCESS`): revenue = Σ`total`, COGS = Σ`costPrice`, profit = Σ`margin`, `count`.
   Grouped by day/month, by product, by category, by store.
2. **`AccountingService`** (new, `transaction` slice or a new `accounting` slice —
   decided in plan; reuses `transactions` table either way): reads aggregates,
   returns report DTOs.
3. **Admin controller + Thymeleaf page**: P&L table, date-range filter, breakdown by
   product/category. SSR-first (initial data inline-seeded; client fetch only for
   filter/pagination changes — matches existing table pattern).
4. **Dashboard tiles**: today's revenue / margin / txn count, added to the existing
   `DashboardController`.

Security: all report endpoints `@PreAuthorize("hasRole('ADMIN')")`.

## Deliberately out of scope (ponytail)

- No new accounting DB schema — reuse `transactions`, query only.
- No double-entry / chart of accounts (Slice 3).
- No rollup/materialized tables — raw SQL aggregates.
  `// ponytail: raw aggregate over transactions, add a daily rollup table if the
  report query gets slow at volume`.
- No caching initially — add `@Cacheable` on report methods only if measurably slow.
- Provider deposit balance tracking moved to Slice 2 (reconciliation) — not needed
  for margin.

## Testing (TDD: Red → Green → Refactor)

- `TransactionDomainServiceTest`:
  - `costPrice` + `margin` set on SUCCESS; both null on FAILED/REFUNDED.
  - Fallback to `basePrice` when provider `cost` is null.
  - Override with provider `cost` when present.
  - `margin == total − costPrice`.
- Repository aggregate test: revenue/COGS/margin/count correct for a date range,
  excluding non-SUCCESS rows.
- Controller/security test: report endpoint is ADMIN-only (403 otherwise).

## Open items for the plan

- Where `AccountingService` + controller live: extend `transaction` slice vs a new
  `accounting` slice. If new module/@Entity is added, remember multi-datasource
  registration (both lists in `CoreDataSourceConfig`) — but Slice 0+1 adds no new
  entity, so likely no datasource change.
- Exact P&L page layout follows the established design-system/landing theme.
