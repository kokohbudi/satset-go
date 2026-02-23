# SatSetGo - Task Board

> **Owner**: August (Senior PM)
> **Last Updated**: 2026-02-22
> **Sprint**: MVP Sprint — Finish Onboarding Test → Purchase Flow

---

## 🔥 CURRENT SPRINT: MVP Critical Path

> **Prinsip**: Fitur yang membuat user bisa **beli pulsa** = MVP. Sisanya backlog.

### Task 6 — Onboarding: Integration & Manual Test ⏳
- [ ] **Test Path A**: Login Google → belum punya Store → muncul form → isi → submit → redirect Dashboard
- [ ] **Test Path B**: Admin tambah reseller → user terima email set-password → login → langsung ke Dashboard (sudah punya store)
- [ ] **Test edge case**: User login, tutup tab sebelum submit form → login lagi → masih redirect ke `/onboarding`
- [ ] **Commit** semua changes dengan message `feat: store-onboarding + keycloak-organization`

### Task 12 — Domain Model: Transactions
- [ ] **Entity `Transactions`**: id, store_id, product_denom_id, target_number, amount, admin_fee, total, status (enum), provider_ref, created/updated audit fields
- [ ] **Enum `TransactionStatus`**: PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED
- [ ] **Repository `TransactionRepository`**: findByStoreId, findByStatus, dll

### Task 13 — Balance di Stores
- [ ] **Tambah field `balance DECIMAL(15,2)` di entity `Stores`** (default 0)
- [ ] **`BalanceService`**: `checkBalance(storeId)`, `deductBalance(storeId, amount)` dengan pessimistic lock, `addBalance(storeId, amount)`
- [ ] **Seed balance** untuk testing (admin set manual via DB atau endpoint sederhana)

### Task 14 — Purchase Service + Mock Provider
- [ ] **Interface `ProviderService`**: `sendTransaction(productCode, targetNumber, amount)` → ProviderResponse
- [ ] **`MockProviderService`**: 90% sukses, delay 500ms-2s, random serial number
- [ ] **`TransactionService`**: `createPurchase(storeId, denomId, targetNumber)` — validasi produk aktif, cek saldo, deduct, panggil provider, update status

### Task 15 — Purchase REST API
- [ ] **`POST /api/transactions/purchase`** — beli produk (storeId, denomId, targetNumber)
- [ ] **`GET /api/transactions/{id}`** — detail transaksi
- [ ] **`GET /api/transactions/history`** — riwayat transaksi per store

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

### Admin Product Management (Week 4)
- [-] AdminProductService (CRUD for categories, products, denoms)
- [-] Admin UI (dashboard, forms, tables)

### Revenue & Pricing (Post-MVP)
- [-] **Reseller Tier & Dynamic Pricing** — Bronze/Silver/Gold/Platinum, naik otomatis dari volume
- [-] **Markup per Store** — setiap Store set markup sendiri di atas harga platform
- [-] **Komisi Upline (Rebate System)** — upline dapat komisi per transaksi downline

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

---

## 🚨 RISKS & MITIGATION

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Balance race condition | Saldo minus, kerugian finansial | HIGH | Pessimistic locking pada deduct balance |
| Provider downtime (mock) | Transaksi stuck | LOW | MockProvider always responds (90% success) |
| No unit tests | Regression bugs saat refactor | MEDIUM | Manual test per task, automated tests post-MVP |
| Onboarding belum di-test end-to-end | Bug saat user pertama kali pakai | MEDIUM | **Task 6 — test sekarang** |

---

## 💬 PM NOTES

**2026-02-22** (August — MVP Re-prioritization):
- **KEPUTUSAN BESAR**: Admin Org Management (Task 7-11) dipindahkan ke BACKLOG
- Alasan: Task 7-11 bukan MVP. User perlu bisa **beli pulsa**, bukan admin manage org
- Purchase Flow (Task 12-17) di-UNLOCK — tidak perlu nunggu org management
- Sprint sekarang: selesaikan Task 6 (test onboarding) → langsung Task 12-17 (purchase)
- Sidebar cleanup sudah dilakukan: hapus 6 menu yang tidak punya controller
- **Target**: User bisa beli pulsa end-to-end dalam sprint ini

**2026-02-20** (August — Admin Org Management breakdown):
- Sprint baru ditambahkan: Admin Organization Management Screen (Task 7-11)
- ~~Estimasi: 3-4 hari kerja~~ → **DEFERRED to backlog (2026-02-22)**

**2026-02-20** (August re-sync dengan Julia.md):
- Store Onboarding jadi prerequisite — Task 1-5 DONE
- Balance scope: **Per Store** (bukan Per User)

**2026-02-12**:
- Tasks.md pertama kali dibuat. Foundation Phase 0 & Week 1 done.

---

**Last Updated**: 2026-02-22
**Next Review**: Setelah Task 6 (onboarding test) selesai
