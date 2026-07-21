# Pascabayar (Postpaid) — Design

**Date:** 2026-07-21
**Feature:** Postpaid bill payment (cek tagihan + bayar) via Digiflazz.
**Slice:** `transaction` (delegates to `digiflazz` client).

---

## Background

Prepaid is single-shot: fixed denom → deduct → provider topup → SN. Postpaid is
two-step and the amount is dynamic:

1. **Inquiry (cek tagihan)** — ask Digiflazz for the current bill of a customer
   number. Read-only, shows amount + customer name. No charge.
2. **Bayar** — pay that bill. Deduct wallet, hit Digiflazz `pay-pasca`, return
   struk/token.

Catalog already models postpaid: `CategoryType.POSTPAID`, `DenomType.OPEN_AMOUNT`,
and on `ProductDenoms`: `requiresInquiry`, `minAmount`, `maxAmount`. So the data
model is mostly in place — this feature adds the transaction flow.

## Digiflazz postpaid API

Same endpoint as prepaid (`POST /v1/transaction`), driven by a `commands` field.
Sign for all = `md5(username + apiKey + ref_id)` (identical to prepaid topup).
`testing:true` honored (canned responses for DF test customer numbers).

| Step        | `commands`    | request fields                                          | key response fields                                             |
|-------------|---------------|---------------------------------------------------------|----------------------------------------------------------------|
| Cek tagihan | `inq-pasca`   | username, buyer_sku_code, customer_no, ref_id, sign     | status, rc, customer_name, `admin`, `price` (bill), selling_price, `desc` (rincian) |
| Bayar       | `pay-pasca`   | same fields, **same ref_id as the paired inquiry**      | status, rc, `sn` (struk/token), price, selling_price           |
| Cek status  | `status-pasca`| same                                                    | status, rc, sn                                                  |

`rc "00"` = success. Pending/Gagal mapping reuses the prepaid rc semantics.

> Note: the Digiflazz docs site blocks automated fetch. The table above is from
> prior integration knowledge; verify any field against a live sandbox call.

## Key decision: re-inquiry at pay time

Do **not** persist inquiry state across the user's think-time. Instead, the pay
operation is self-contained and re-inquires right before charging:

- The display inquiry (Phase 1) is stateless — returns the bill, stores nothing.
- At pay (Phase 2), generate a fresh `ref`, call `inq-pasca` again with it to get
  the **current** bill, then `pay-pasca` with the **same** `ref`. Digiflazz ties
  the payment to the immediately-preceding inquiry on that ref_id.

Consequences: no `postpaid_inquiries` table, no `INQUIRED` intermediate status, no
stale-bill risk. The charge is always the freshly-fetched amount. One ledger
(`Transactions`), consistent with the existing one-ledger decision.

## Product types: full-bill vs input-amount

Per Digiflazz docs, `pay-pasca` has **no amount param** — pure pascabayar always
pays the full inquired bill. But `inq-pasca` **accepts an `amount`** for certain
products (e-money top-up), which is the "input nominal" case. Our catalog already
carries the discriminators (`DenomType`, `minAmount`, `maxAmount`, `requiresInquiry`),
so the flow is driven by the denom, not hardcoded:

| Denom                              | Flow                                                        |
|------------------------------------|-------------------------------------------------------------|
| `FIXED_DENOM` + `requiresInquiry`  | pasca murni: inquiry (no amount) → pay **full bill**         |
| `OPEN_AMOUNT` + `requiresInquiry`  | e-money: user inputs nominal (validate `minAmount`/`maxAmount`) → inquiry(**amount**) → pay |
| `FIXED_DENOM`, no inquiry          | existing prepaid direct-pay (unchanged)                      |

Implication: `inquiry(...)` and `pay(...)` take an **optional `amount`**
(`BigDecimal`, null for full-bill). It is sent to DF only when the denom is
`OPEN_AMOUNT`; for `FIXED_DENOM` it must be null (server rejects a non-null amount
on a fixed denom, and rejects a null/out-of-range amount on an open denom).

---

## Architecture / boundaries

- **New `PostpaidService` with `@LogContext("Postpaid")`** in the `transaction`
  slice → business trace routes to `logs/Postpaid/`. Controller never calls the
  client directly (outbound-via-service rule). HTTP raw logging stays centralized
  in `ProviderHttpConfig` → `logs/supplier/`.
- **`ProviderPort`** (external boundary → interface justified) gains:
  - `InquiryResult inquiry(String customerNo, String denomCode, String refId)`
  - `ProviderResponse payPostpaid(String customerNo, String denomCode, String refId)`
- **`RealProviderAdapter`** delegates both to new `DigiflazzClient` methods and
  maps status/rc to `ProviderStatus` (reusing existing `mapStatus` logic).
- **`DigiflazzClient`** gains `inquiry()` / `payPostpaid()`. Same `POST /transaction`,
  request record gains a `commands` field. Sign unchanged.

## New types

- `InquiryResult` (read model): `customerName`, `bill`, `admin`, `rc`, `message`,
  `desc` (raw rincian passthrough as `JsonNode`/`Map`). Kept separate from
  `ProviderResponse` (which stays the pay/topup result shape).

---

## Phase 1 — Inquiry (backend, read-only)

- `POST /api/transactions/inquiry` — body `{denomId, customerNo, amount?}`.
- `PostpaidService.inquiry(denomId, customerNo, amount)`:
  - Load `DenomInfo`; require `requiresInquiry`; reject if unavailable.
  - **Amount rule**: `OPEN_AMOUNT` → `amount` required and within `minAmount`/`maxAmount`;
    `FIXED_DENOM` → `amount` must be null. Violation → `BusinessException`.
  - `ref = refNoGenerator.next()` (throwaway for display).
  - `provider.inquiry(customerNo, denom.code(), ref, amount)` (amount sent to DF
    only for `OPEN_AMOUNT`).
  - Compute `total = bill + dfAdmin + denom.adminFee` (our markup).
  - Return DTO: `customerName, bill, dfAdmin, ourMarkup(=denom.adminFee), total, rincian`.
- No DB row, no deduct. Inquiry failure → error response to UI, zero side effects.

## Phase 2 — Pay (backend, money path)

`PostpaidService.pay(denomId, customerNo, amount, expectedTotal)`:

1. Load `DenomInfo` (validate `requiresInquiry` + available + same amount rule as Phase 1).
2. Double-submit guard: reuse
   `existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter`.
3. `ref = refNoGenerator.next()` — used for **both** DF calls this attempt.
4. `inqNow = provider.inquiry(customerNo, denom.code(), ref, amount)` — fresh bill + dfAdmin.
   Inquiry failure → abort, no charge.
5. `total = inqNow.bill + inqNow.admin + denom.adminFee`.
6. **Mismatch guard**: if `expectedTotal != total` → `409` (bill changed since the
   displayed inquiry). UI re-confirms. No silent charge-amount change.
7. Create `Transactions` row `PROCESSING`: `targetNumber=customerNo`, `price=bill`,
   `adminFee=dfAdmin + markup`, `total`, `refNo=ref`, `customerName`.
8. `deduct(walletId, total, txId, ...)`.
9. `payResp = provider.payPostpaid(customerNo, denom.code(), ref)` — same ref, DF
   ties to the step-4 inquiry, no double-charge. `pay-pasca` sends **no amount**
   (the ref-bound inquiry already fixed it).
10. **Reuse `reconcileProviderResult(tx, payResp, walletId, denom)`**:
    - SUCCESS → `sn` = struk/token, `cost` = `payResp.price` (DF's price = our cost).
    - PENDING → stays PROCESSING, poll settles (money-safe, existing path).
    - FAILED → refund; refund-fail leaves FAILED for Ops (existing path).

`POST /api/transactions/pay` — body `{denomId, customerNo, amount?, expectedTotal}`.

## Phase 3 — UI (reuse existing grid, no new page)

**Reuse the existing category/product/denom selection UI.** No new standalone
page. The only change is the flow *after* a denom is picked, branched on the
denom's `requiresInquiry` + `DenomType`:

- `requiresInquiry == false` → **existing prepaid direct-pay** (unchanged).
- `requiresInquiry == true`, `FIXED_DENOM` → input `customerNo` → `fetch`
  `/api/transactions/inquiry` (no amount) → bill card (nama pelanggan, tagihan,
  admin, total) → confirm → `fetch` `/api/transactions/pay` (send `expectedTotal`)
  → struk / SN, or re-confirm on 409.
- `requiresInquiry == true`, `OPEN_AMOUNT` → input `customerNo` **+ nominal**
  (validate `minAmount`/`maxAmount` client-side, server re-validates) → `fetch`
  inquiry(amount) → total card → confirm → pay(amount, expectedTotal).

The denom payload the page already loads must expose `requiresInquiry`,
`denomType`, `minAmount`, `maxAmount` so the client can branch. Follows the
existing SSR-first + Alpine.js + `fetch`-mutasi pattern.

---

## Data model changes

- `Transactions`: **add `customerName`** (nullable, `length 100`) only.
  - `price` = bill, `adminFee` = dfAdmin + our markup, `sn` = struk/token.
  - `desc`/rincian **not persisted** — ephemeral in the inquiry API response only.
- No new tables, no new status values.

## Pricing / markup

- `denom.adminFee` = **our** reseller markup, added on top of DF's `bill + admin`.
- Cost (`costPrice`) on success = `payResp.price` (what DF charged us); margin =
  `total - cost`, computed by the existing reconcile.

## Error handling

- Reuse `SupplierException` + existing rc → `ProviderStatus` mapping.
- Inquiry failure: surface message to UI, no charge, no row.
- Pay HTTP failure: `payPostpaid` returns null status → PENDING → row stays
  PROCESSING → reconcile poll re-hits DF and settles (existing money-safe pattern).
- Never expose raw `e.getMessage()` to client (log only).

## Testing (TDD, red→green→refactor)

- `PostpaidService.inquiry` — mock `ProviderPort`: happy path, denom not postpaid,
  unavailable, supplier error.
- `PostpaidService.pay` — mock: success, mismatch-guard 409, inquiry-fail abort,
  double-submit block, pay PENDING (no refund), pay FAILED (refund).
- `DigiflazzClient.inquiry/payPostpaid` — parse canned `inq-pasca`/`pay-pasca` JSON
  (success, pending, gagal, error-object).
- `RealProviderAdapter` — status/rc mapping for inquiry + pay.

## Out of scope (YAGNI)

- Separate postpaid ledger table / inquiry persistence / rincian storage
  (re-inquiry + one-ledger remove the need).
- `status-pasca` polling wiring — reuse the existing prepaid reconcile poll if it
  already re-POSTs by ref; add postpaid status command only if that poll can't
  settle postpaid. Flag during Phase 2.

## Phasing

1. Backend inquiry (Phase 1).
2. Backend pay (Phase 2).
3. UI (Phase 3).

Each phase: TDD, all tests green, no regressions before moving on.
