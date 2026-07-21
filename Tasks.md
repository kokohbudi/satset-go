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

- [ ] **WH-0**: Cek dokumentasi Digiflazz — apakah mereka expose webhook/callback utk update status transaksi (endpoint URL registration, payload format, signature scheme)
- [ ] **WH-1**: `POST /api/webhooks/digiflazz` endpoint — terima callback status transaksi
- [ ] **WH-2**: Verifikasi signature/secret webhook (JANGAN percaya payload tanpa validasi — cegah spoofed status update dari luar)
- [ ] **WH-3**: Wire ke `TransactionDomainService.reconcileProviderResult(...)` — mapping payload webhook → `ProviderResponse`
- [ ] **WH-4**: Idempotency — webhook bisa kekirim >1x (retry Digiflazz), pastiin re-delivery gak dobel proses (cek status transaksi udah final sebelum apply)
- [ ] **WH-5**: `@LogContext("Webhook")` di service webhook (outbound/inbound trace policy — lihat CLAUDE.md)
- [ ] **WH-6**: Test — signature invalid ditolak, replay/duplicate delivery idempotent, payload PENDING/SUCCESS/FAILED ke-handle bener
- [ ] **WH-7**: (optional, kalau Digiflazz webhook delivery gak reliable) fallback safety net — cron jarang (misal 1x/jam) buat catch transaksi yg kelewat webhook, BUKAN polling agresif kayak yg lama

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

### Transaction ref_no
- [-] Fixed-length ref_no — sequence global `tx_ref_seq` pad `%05d` jadi 6+ digit setelah lewat 99.999 (panjang goyang). Opsi A: lebarin pad `%09d` (1 char, tetap global). Opsi B: reset harian via `tx_ref_counter` table (nomor urut per-hari, 5 digit cukup). Pilih A kalau cuma mau panjang stabil, B kalau CS butuh urut harian. Lihat `RefNoGenerator.java`.

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
