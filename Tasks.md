# SatSetGo - Task Board

> **Owner**: August (Senior PM)
> **Last Updated**: 2026-02-24 15:46
> **Sprint**: MVP Sprint — Purchase Flow UI & Testing

---

## 🔥 CURRENT SPRINT: MVP Critical Path

> **Prinsip**: Fitur yang membuat user bisa **beli pulsa** = MVP. Sisanya backlog.

### Task 6 — Onboarding: Integration & Manual Test ✅
- [x] **Test Path A**: Login Google → belum punya Store → muncul form → isi → submit → redirect Dashboard
- [x] **Test Path B**: Admin tambah reseller → user terima email set-password → login → langsung ke Dashboard (sudah punya store)
- [x] **Test edge case**: User login, tutup tab sebelum submit form → login lagi → masih redirect ke `/onboarding`
- [x] **Commit** semua changes dengan message `feat: store-onboarding + keycloak-organization`

### Task 15b — Purchase API (Missing Endpoints) ✅
- [x] **`GET /api/transactions/{id}`** — detail transaksi
- [x] **`GET /api/transactions/history`** — riwayat transaksi per store

### Task 16 — Purchase UI
- [ ] **Purchase form**: pilih produk → input nomor → konfirmasi → submit
- [ ] **Success/error page**: status transaksi, serial number (jika sukses)
- [ ] **Transaction history page**: tabel riwayat transaksi

### Task 17 — Integration Test Purchase Flow
- [ ] **Test happy path**: pilih produk → beli → saldo terpotong → transaksi SUCCESS
- [ ] **Test saldo tidak cukup**: error message, saldo tidak berubah
- [ ] **Test provider gagal**: transaksi FAILED → saldo refund otomatis

---

## 📦 BACKLOG (GOOD IDEA, NOT NOW)

### Admin Organization Management (Post-MVP)
> *Dipindahkan dari "UP NEXT" — bukan MVP blocker*
- [-] Task 7-11: KC Org API read/update, AdminOrgService, Admin Org UI, members modal, edit business data
- [-] User segregation backoffice vs reseller (brainstorm Option A/B/C sudah documented)

### Admin User Management Cleanup (Post-MVP)
- [-] Fix `/admin/user-management` — filter hanya tampilkan backoffice users
- [-] Sidebar: menu `/users`, `/groups` sudah dihapus (tidak ada page controller)

### Balance Top-up (Week 3+)
- [-] Payment service entities (Deposits, PaymentTransactions)
- [-] MockPaymentGateway implementation
- [-] Top-up UI with payment flow
- [-] **Store Mutations UI**: Tab riwayat mutasi saldo di menu /transactions untuk melihat top-up, potongan pembelian, dan refund

### Admin Product Management (Week 4)
- [-] AdminProductService (CRUD for categories, products, denoms)
- [-] Admin UI (dashboard, forms, tables)

### Revenue & Pricing (Post-MVP)
- [-] **Reseller Tier & Dynamic Pricing** — Bronze/Silver/Gold/Platinum, naik otomatis dari volume
- [-] **Markup per Store** — setiap Store set markup sendiri di atas harga platform
- [-] **Komisi Upline (Rebate System)** — upline dapat komisi per transaksi downline

### 🏗️ Hexagonal Architecture + Unit Tests (Post-Purchase UI) ⭐ HIGH PRIORITY
> *Refactor arsitektur flat → Hexagonal (Ports & Adapters) + comprehensive unit tests*
- [-] Domain layer: pure business logic, zero framework dependency
- [-] Port in (use cases): `PurchasePrepaidUseCase`, `TopUpUseCase`, `ViewHistoryUseCase`
- [-] Port out (adapters): `TransactionPort`, `BalancePort`, `ProviderPort`
- [-] Adapter in/web: Controllers
- [-] Adapter out/persistence: JPA repositories
- [-] Adapter out/provider: MockProvider, RealProvider
- [-] Unit tests edge cases:
  - Saldo pas-pasan → SUCCESS, balance = 0
  - Saldo kurang Rp 1 → REJECTED, balance unchanged
  - Provider timeout → FAILED → auto refund
  - Double submit (idempotency) → second rejected
  - Concurrent purchase (2 thread, 1 saldo) → hanya 1 berhasil
  - Purchase denom inactive/deleted → REJECTED
  - Refund gagal setelah provider fail → alert

### Reseller Experience (Post-MVP)
- [-] **White-label Storefront** — setiap Store punya URL sendiri
- [-] **Dashboard Analytics per Store** — total transaksi, produk terlaris, profit bulanan
- [-] **API Key untuk Reseller** — integrasi via REST API untuk volume tinggi
- [-] **Bulk/Batch Transaction** — upload CSV untuk kirim pulsa massal

### Platform & Operations (Future)
- [-] **Auto-switch Supplier (Failover)**
- [-] **Product Price Watcher**
- [-] **Dispute & Complaint Management**
- [-] **Audit Log & Activity Trail**

### Product Expansion (Future)
- [-] **Postpaid Inquiry**, **Produk Non-Telco**, **Real Provider Integration**

### Growth & Engagement (Future)
- [-] **Promo & Voucher Engine**, **Notification Engine**, **Gamification**, **Referral Tracking**

---

## ✅ DONE (CELEBRATE!)

### Phase 0: Foundation (2026-02-12)
- [x] Database design brainstorming (Hybrid Approach selected)
- [x] Entities created: Categories, Products, ProductDenoms, ProductDenomMeta
- [x] Enums created: CategoryType, DenomType
- [x] Repositories with caching & filtering
- [x] Migrated @GenericGenerator → @UuidGenerator (modern Hibernate pattern)
- [x] Documentation: ROADMAP.md, CLAUDE.md

### Week 1: Browse Products (2026-02-12)
- [x] Service Layer (CategoryService, ProductService, ProductDenomService)
- [x] DTO mapping (CategoryDTO, ProductDTO, ProductDenomDTO)
- [x] REST API (ProductCatalogController) - 5 endpoints with caching
- [x] UI (ProductPageController + Thymeleaf) - category browsing, product grid, denom listing
- [x] Responsive layout with Tailwind CSS

### Store Onboarding — Task 1-5 (2026-02-20)
- [x] Stores entity updated (keycloakOrganizationId, phone, LocalDateTime migration)
- [x] Keycloak Organization API (createOrganization, addMember, createResellerUser)
- [x] StoreOnboardingInterceptor + WebMvcConfig
- [x] StoreOnboardingService + AdminOnboardingService
- [x] OnboardingController + reseller-form.html
- [x] Sidebar cleanup: hapus menu tanpa controller (/users, /groups, /user-groups, /transactions, /deposit, /settings)

### Purchase Prepaid — Task 12-15b (2026-02-24)
- [x] **Task 12**: Entity `Transactions` + `StoreMutations` (Double-Entry Ledger) + 3 enums
- [x] **Task 13**: `Stores.balance` (cache) + `BalanceService` (pessimistic lock + ledger)
- [x] **Task 14**: `ProviderService` interface + `MockProviderService` (90% success, 500ms delay)
- [x] **Task 15**: `TransactionService` (saga: deduct → provider → refund) + `TransactionController` (purchase, topup, balance)
- [x] **Task 15b**: `TransactionDTO` + `GET /{id}` + `GET /history` endpoints
- [x] Arsitektur: **Double-Entry Ledger** (buku tabungan) — `StoreMutations` = source of truth, `Stores.balance` = read cache
- [x] Polymorphic reference (`referenceType` + `referenceId`) untuk fleksibilitas mutasi

---

## 🚨 RISKS & MITIGATION

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Balance race condition | Saldo minus, kerugian finansial | HIGH | Pessimistic locking pada deduct balance |
| Provider downtime (mock) | Transaksi stuck | LOW | MockProvider always responds (90% success) |
| No unit tests | Regression bugs saat refactor | MEDIUM | Manual test per task, automated tests post-MVP |
| Onboarding belum di-test end-to-end | Bug saat user pertama kali pakai | ~~MEDIUM~~ | ✅ **Task 6 — DONE** |

---

## 💬 PM NOTES

**2026-02-24 15:46** (August — Task 6 & 15b Done!):
- **Task 6 SELESAI** — Onboarding integration test sudah dilakukan (Path A, Path B, edge case)
- **Task 15b SELESAI** — 2 missing endpoint sudah ditambahkan:
  - `GET /api/transactions/{id}?storeId=...` (detail transaksi, secured by storeId)
  - `GET /api/transactions/history?storeId=...` (riwayat per store, sorted newest)
  - `TransactionDTO` (Java record) sebagai response DTO
- **`mvn compile` BUILD SUCCESS** — zero errors
- **Next**: Task 16 (Purchase UI) → Task 17 (Integration Test)
- Sprint focus sekarang: **Task 16 + 17 saja** (Task 6 & 15b sudah clear)

**2026-02-24** (August — Purchase Flow Backend Done!):
- **Task 12-15 SELESAI** — Backend purchase prepaid sudah full functional
- **Keputusan arsitektur baru**: Double-Entry Ledger (buku tabungan) menggantikan design awal
  - `StoreMutations` = source of truth (immutable ledger)
  - `Stores.balance` = read cache (sync otomatis saat mutasi)
  - Polymorphic `referenceType + referenceId` (bukan hardcoded FK ke Transactions)
- **3 endpoint siap**: `POST /purchase`, `POST /topup`, `GET /balance/{storeId}`
- ~~Missing endpoint: `GET /{id}` dan `GET /history`~~ → **DONE (Task 15b)**

**2026-02-22** (August — MVP Re-prioritization):
- **KEPUTUSAN BESAR**: Admin Org Management (Task 7-11) dipindahkan ke BACKLOG
- Alasan: Task 7-11 bukan MVP. User perlu bisa **beli pulsa**, bukan admin manage org
- Purchase Flow (Task 12-17) di-UNLOCK — tidak perlu nunggu org management
- Sidebar cleanup sudah dilakukan: hapus 6 menu yang tidak punya controller

**2026-02-20** (August — Admin Org Management breakdown):
- Sprint baru ditambahkan: Admin Organization Management Screen (Task 7-11)
- ~~Estimasi: 3-4 hari kerja~~ → **DEFERRED to backlog (2026-02-22)**

**2026-02-20** (August re-sync dengan Julia.md):
- Store Onboarding jadi prerequisite — Task 1-5 DONE
- Balance scope: **Per Store** (bukan Per User)

**2026-02-12**:
- Tasks.md pertama kali dibuat. Foundation Phase 0 & Week 1 done.

---

**Last Updated**: 2026-02-24 15:46
**Next Review**: Setelah Task 16 (Purchase UI) selesai

