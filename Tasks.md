# SatSetGo - Task Board

> **Owner**: August (Senior PM)
> **Last Updated**: 2026-07-18
> **Sprint**: WH-series (Digiflazz webhook) + CAT-PERM (catalog permissions) — prioritas

---

## 🔥 CURRENT SPRINT

### Digiflazz Webhook — WH-series
> **Goal**: ganti polling reconciler (`TransactionReconcileService`, `@Scheduled` tiap 60s, dihapus 2026-07-18)
> dengan callback push dari Digiflazz. Status PROCESSING gak lagi di-re-POST aktif, nunggu webhook.
> `TransactionDomainService.reconcileProviderResult(...)` tetap dipakai sbg settlement logic — webhook handler
> tinggal panggil ini, bukan re-implement.

- [x] **WH-0**: Cek dokumentasi Digiflazz — apakah mereka expose webhook/callback utk update status transaksi (endpoint URL registration, payload format, signature scheme). Hasil: `developer.digiflazz.com/api/buyer/webhook/` — HMAC-SHA1 raw body, header `X-Hub-Signature: sha1=<hex>`, registrasi manual di dashboard DF (Atur Koneksi > API > Webhook).
- [x] **WH-1**: `POST /api/webhooks/digiflazz` endpoint — terima callback status transaksi. Deploy standalone (`satset-webhook` module, Fly.io — https://satset-webhook.fly.dev), reuse `satset-core` domain code langsung (bukan HTTP), fold ke core pas core deploy prod. Design: `docs/superpowers/specs/2026-07-20-webhook-split-deploy-design.md`.
- [x] **WH-2**: Verifikasi signature/secret webhook — `DigiflazzSignatureVerifier` (HMAC-SHA1, raw body).
- [x] **WH-3**: Wire ke `TransactionDomainService.reconcileProviderResult(...)` — `DigiflazzWebhookService` + `DigiflazzWebhookPayload.toProviderResponse()` (reuse `DigiflazzStatusMapper`, di-extract dari `RealProviderAdapter`).
- [x] **WH-4**: Idempotency — guard di `reconcileProviderResult` (no-op kalau transaksi udah SUCCESS/FAILED/REFUNDED).
- [x] **WH-5**: `@LogContext("Webhook")` di `DigiflazzWebhookService`.
- [x] **WH-6**: Test — 28/28 pass (`satset-webhook` module): signature valid/invalid, payload mapping, idempotency guard, full HTTP→DB integration (Sukses/Gagal/replay/bad-sig/unknown-ref/malformed).
- [ ] **WH-7**: (optional, kalau Digiflazz webhook delivery gak reliable) fallback safety net — cron jarang (misal 1x/jam) buat catch transaksi yg kelewat webhook, BUKAN polling agresif kayak yg lama
- [ ] **WH-8**: Register URL `https://satset-webhook.fly.dev/api/webhooks/digiflazz` di dashboard DF (Atur Koneksi > API > Webhook), ambil/pasang secret final, set `DIGIFLAZZ_WEBHOOK_SECRET` Fly secret, verify pake ping endpoint DF (`POST /v1/report/hooks/[ID]/pings`)
- [ ] **WH-9**: Postpaid webhook support — DF kirim prepaid/postpaid/hotel ke URL **yang sama**, dibedain lewat header `User-Agent` (`Digiflazz-Hookshot`/`Digiflazz-Pasca-Hookshot`/`Digiflazz-Hotel-Hookshot`). Skarang kode assume semua payload prepaid shape, gak cek `User-Agent` — aman selama akun cuma jalanin prepaid. Kalau postpaid/hotel diaktifin: tambah check `User-Agent` di `DigiflazzWebhookController`, reject/route beda kalau bukan `Digiflazz-Hookshot`, plus payload shape postpaid beda (perlu DTO/mapping sendiri)

### Catalog Permissions — CAT-PERM
> Split view-only vs edit permission (categories/products/denoms/prices), rationalize `@PreAuthorize`
> di `AdminCatalogController` + `CatalogSyncController`. Role source: Keycloak (`SatsetConstants.PERM_*`).

- [ ] **CAT-PERM**: Redesign catalog permissions — split view-only vs edit (categories/products/denoms/prices). Rationalize `@PreAuthorize` (`AdminCatalogController` + `CatalogSyncController`, `SatsetConstants.PERM_*`) + template `canManageCat/Prod/Denom` flags. Do in main checkout.

---

## 📦 BACKLOG

### Balance Top-up
- [-] MockPaymentGateway — masuk ke satset-wallet (bukan Core)
- [-] Top-up UI di Core — call Wallet credit endpoint
- [-] Store Mutations UI — riwayat mutasi saldo di `/transactions`, fetch dari Wallet API

### Admin Organization Management (Post-MVP)
- [-] Task 7-11: KC Org API read/update, AdminOrgService, Admin Org UI, members modal, edit business data
- [-] User segregation backoffice vs reseller

### Admin User Management Cleanup
- [-] `/admin/user-management` — filter hanya backoffice users
- [-] Refactor User Search — migrasi Keycloak Admin API → DB lokal (database-level pagination)

### Revenue & Pricing (Post-MVP)
- [-] Reseller Tier & Dynamic Pricing (Bronze/Silver/Gold/Platinum)
- [-] Markup per Store
- [-] Komisi Upline (Rebate System)

### Technical Debt
- [ ] **C-1**: Separate domain models dari JPA `@Entity` — pure domain class + JPA entity + mapper per bounded context
- [ ] **L-8**: Pagination untuk product listing (low urgency, tunggu >100 produk)
- [ ] **INF-series**: HikariCP Virtual Thread stress test — validate apakah perlu migrasi ke Agroal

### Future
- [-] White-label Storefront, Dashboard Analytics, API Key Reseller, Bulk Transaction
- [-] Auto-switch Supplier, Dispute Management, Audit Log
- [-] Postpaid Inquiry, Real Provider Integration
- [-] Promo Engine, Notification, Gamification, Referral

---

## ✅ DONE (Summary)

| Series | Keterangan | Selesai |
|--------|-----------|---------|
| MVP (Task 1-17) | Foundation, Onboarding, Purchase flow, Integration test | 2026-02-25 |
| H-series | Port boundary cleanup (3 items) | 2026-02-25 |
| M-series | Config + correctness + hygiene (10 items) | 2026-02-25 |
| L-series | Code hygiene — L-1..L-7, L-9 (L-8 pending) | 2026-03-01 |
| AP-series | Admin Product Management — backend + frontend + tests + KC roles | 2026-03-02 |
| AP-N series | Catalog drill-down navigation | 2026-03-02 |
| OJ-series | JWT Org ID — CLOSED (DB lookup cukup, tidak perlu JWT) | 2026-03-03 |
| MR-series | Mandatory Role Assignment (Path C) | 2026-03-03 |
| C-2 | Decouple cross-context JPA FK → UUID references (339 tests pass) | 2026-03-06 |
| L-1 | Unit test coverage 68% → 94% instruction (+34 tests) | 2026-03-05 |
| WR-series | Wallet Refactor — WalletAccount entity, ports, BalanceDomainService (413 tests pass) | 2026-03-06 |
| W-SETUP + Phase A-C | Multi-module Maven, Hexagonal fix, Test fix (413 tests pass) | 2026-03-07 |
| WI-series | Single-save store creation (pre-generate UUID) | 2026-03-07 |
| W-1..W-6 | satset-wallet service + WalletClientAdapter aktif | 2026-03-10 |
| W-7, W-8, TX-series | **CANCELLED (YAGNI)** — wallet-split gak jadi, wallet direabsorb in-process ke satset-core (`WalletGateway`→`WalletService`, JPA langsung, no HTTP/token-exchange) | 2026-07-18 |
