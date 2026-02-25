# SatSetGo - Task Board

> **Owner**: August (Senior PM)
> **Last Updated**: 2026-02-25
> **Sprint**: MVP Sprint — Testing & Refactoring

---

## 🔥 CURRENT SPRINT: MVP Critical Path

> **Prinsip**: Fitur yang membuat user bisa **beli pulsa** = MVP. Sisanya backlog.

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
- [x] Domain layer: pure business logic, zero framework dependency
- [x] Port in (use cases): `PurchasePrepaidUseCase`, `TopUpUseCase`, `ViewHistoryUseCase`
- [x] Port out (adapters): `TransactionPort`, `BalancePort`, `ProviderPort`
- [x] Adapter in/web: Controllers
- [x] Adapter out/persistence: JPA repositories
- [x] Adapter out/provider: MockProvider, RealProvider
- [x] Unit tests for Identity (`UserDomainService`, `IdentityDomainService`)
- [x] Unit tests edge cases (Transaction):
  - [x] Saldo pas-pasan → SUCCESS, balance = 0
  - [x] Saldo kurang Rp 1 → REJECTED, balance unchanged
  - [x] Provider timeout → FAILED → auto refund
  - [x] Double submit (idempotency) → second rejected
  - [x] Concurrent purchase (2 thread, 1 saldo) → hanya 1 berhasil
  - [x] Purchase denom inactive/deleted → REJECTED
  - [x] Refund gagal setelah provider fail → alert

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

### Self-Service Password Change (2026-02-25)
- [x] `ChangeMyPasswordRequestDTO` (Bean Validation: newPassword, confirmPassword)
- [x] `ManageMyProfileUseCase` (port in) + `UserSelfServiceDomainService` (domain service)
- [x] `UserSelfServiceController` — dual auth principal (OidcUser + Jwt), session-aware
- [x] `ProfileController` (GET /profile, POST /profile/change-password) — secured
- [x] `profile.html` Thymeleaf UI — user info card + password change form + toggle show/hide password
- [x] Build verification: `mvn clean package -DskipTests=true` → **BUILD SUCCESS**

### Code Cleanup & Refinement (2026-02-25)
> *Hapus dead code, simplify password flow, IDE cleanup*
- [x] **Role Attributes dihapus** — `role-attributes.html`, endpoint API, service logic, port/use case methods (~400 baris dibuang)
- [x] **Password Change disederhanakan** — `oldPassword` field dihapus (user sudah authenticated via session, tidak perlu ROPC verification)
- [x] **Profile UI enhanced** — toggle show/hide password (eye icon) untuk newPassword & confirmPassword
- [x] **IDE files cleanup** — `.idea/.gitignore`, `omnip-services.iml` dihapus dari tracking

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

**2026-02-25** (August — Code Cleanup & Password Refinement):
- **Role Attributes DIHAPUS** — Fitur ini dead code, tidak ada use case di MVP. ~400 baris dibuang dari 7 file (controller, service, port, adapter, template).
- **Password Change DISEDERHANAKAN** — `oldPassword` dihapus. Reasoning: user sudah login via session, ROPC verification overkill untuk UI flow. Lebih simpel, lebih aman (tidak perlu kirim password lama via JS).
- **Profile UI** — Toggle show/hide password ditambahkan. UX improvement kecil tapi penting.
- **Dual Auth Principal** — `UserSelfServiceController` sekarang support `OidcUser` (UI session) + `Jwt` (API token). Lebih robust.
- ⚠️ **Perubahan belum di-commit** — 14 files changed, 41 insertions, 481 deletions.
- **Next**: Commit cleanup, lalu Task 17 (Integration Test Purchase Flow).

**2026-02-25** (August — Self-Service Password Change Done!):
- **Self-Service Password Change SELESAI** — User bisa ganti password sendiri dari halaman `/profile` tanpa butuh Admin.
- Arsitektur: Hexagonal (Controller → UseCase → DomainService → KeycloakPort).
- `mvn clean package -DskipTests=true` → **BUILD SUCCESS** (119 source files compiled, 0 errors).
- **Next**: ~~Task 17 (Integration Test Purchase Flow)~~ → Code cleanup dulu.

**2026-02-24 18:57** (August — Purchase UI Done!):
- **Task 16 SELESAI** — Purchase UI dan Transaction history UI sudah diimplementasikan beserta controller-nya (TransactionPageController).
- **Next**: Task 17 (Integration Test Purchase Flow) dan Unit Test Transaction.

**2026-02-24 18:55** (August — Hexagonal & Identity Tests Done!):
- **Hexagonal Architecture Refactor SELESAI** — Bounded contexts (Transaction, Catalog, Identity, Onboarding) sudah pakai Ports & Adapters.
- **Identity Unit Tests SELESAI** — Test untuk `UserDomainService` dan `IdentityDomainService` dengan Mockito.
- **Role Management Refined** — Realm roles vs Client roles separation (khusus backoffice vs reseller).
- **Tailwind CSS Fixed** — Bug font-sans berhasil di-resolve, bloker Purchase UI sudah hilang.
- **Next**: Melanjutkan Task 16 (Purchase UI) dan menyelesaikan Unit Test Transaction.

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

**Last Updated**: 2026-02-25 (Session 2)
**Next Review**: Commit cleanup → Task 17 (Integration Test Purchase Flow)

