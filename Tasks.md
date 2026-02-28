# SatSetGo - Task Board

> **Owner**: August (Senior PM)
> **Last Updated**: 2026-02-25
> **Sprint**: MVP Sprint — Testing & Refactoring

---

## 🔥 CURRENT SPRINT: MVP Critical Path

> **Prinsip**: Fitur yang membuat user bisa **beli pulsa** = MVP. Sisanya backlog.

### ~~Task 17 — Integration Test Purchase Flow~~ ✅ DONE
- ✅ **Test happy path**: pilih produk → beli → saldo terpotong → transaksi SUCCESS
- ✅ **Test saldo tidak cukup**: error message (HTTP 422), saldo tidak berubah, provider tidak dipanggil
- ✅ **Test provider gagal**: transaksi REFUNDED → saldo dikembalikan otomatis

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
- ✅ Domain layer: pure business logic, zero framework dependency
- ✅ Port in (use cases): `PurchasePrepaidUseCase`, `TopUpUseCase`, `ViewHistoryUseCase`
- ✅ Port out (adapters): `TransactionPort`, `BalancePort`, `ProviderPort`
- ✅ Adapter in/web: Controllers
- ✅ Adapter out/persistence: JPA repositories
- ✅ Adapter out/provider: MockProvider, RealProvider
- ✅ Unit tests for Identity (`UserDomainService`, `IdentityDomainService`)
- ✅ Unit tests edge cases (Transaction):
  - ✅ Saldo pas-pasan → SUCCESS, balance = 0
  - ✅ Saldo kurang Rp 1 → REJECTED, balance unchanged
  - ✅ Provider timeout → FAILED → auto refund
  - ✅ Double submit (idempotency) → second rejected
  - ✅ Concurrent purchase (2 thread, 1 saldo) → hanya 1 berhasil
  - ✅ Purchase denom inactive/deleted → REJECTED
  - ✅ Refund gagal setelah provider fail → alert
- ✅ **Dependency rule enforcement**: Domain services inject port interfaces only (zero `adapter.out` imports)
  - ✅ Catalog: `CategoryDomainService`, `ProductDomainService`, `DenomDomainService`
  - ✅ Identity: `UserDomainService`, `IdentityDomainService`, `UserManagementHelper`
  - ✅ Onboarding: `StoreDomainService`, `AdminOnboardingDomainService`, `StoreOnboardingDomainService`, `RegistrationHelper`
  - ✅ Transaction: `TransactionDomainService`, `BalanceDomainService`
  - ✅ Unit test `TransactionDomainServiceTest` uses port interfaces

### ✅ Technical Debt — Port Boundary Cleanup (H-1, H-3, H-6) DONE
> *Surgical fixes, completed in 1 session (2026-02-25)*
- [x] **H-1**: `shared` package (`Beans.java`, `StoreOnboardingInterceptor.java`, `DataSeeder.java`) — JPA adapter repos replaced with port interfaces ✅
- [x] **H-3**: `KeycloakIdentityPort` — `UserRepresentation` replaced with domain record `GroupMemberInfo` at boundary ✅
- [x] **H-6**: `UserDTO` — `Stores` entity reference replaced with `storeId` (UUID), updated in 5 consumers ✅

### 🔧 Technical Debt — Correctness Quick Wins (M-1, M-4, M-8)
> *Quick fixes, masing-masing <15 menit. Bisa dikerjakan kapan saja.*
- [x] **M-1**: Tambah `@Version` ke `Transactions.java` — race condition on status update ✅
- [x] **M-4**: `RegistrationHelper.java:24` — ganti `java.util.Random` → `SecureRandom` untuk referral IDs ✅
- [x] **M-8**: `CategoryDomainService` — fix cache name collision (`findAll()` dan `findByType()` share cache `"categories"`) ✅

### ✅ Technical Debt — Config & Security Formalization (M-3, M-5, M-6, M-10) DONE
> *Config-level fixes, completed in 1 session (2026-02-25)*
- [x] **M-3**: `Stores.balance` locking strategy documented — dual approach: pessimistic lock in `BalanceDomainService` (financial ops), optimistic lock `@Version` (general concurrency) ✅
- [x] **M-5**: Role prefixes formalized — `ROLE_PREFIX_REALM`, `ROLE_PREFIX_CLIENT` + 7 permission constants in `OmniConstants`; @PreAuthorize updated across 4 controllers ✅
- [x] **M-6**: Virtual threads cleanup — `max-threads: 200` & `min-spare-threads: 10` removed from `application.yml` ✅
- [x] **M-10**: Config externalization — all secrets moved to `.env`, single `application.yml` with env variable references, `application-secret.yml` deleted ✅

### 🏗️ Technical Debt — Domain Model Separation (C-1, C-2) 🔴 EPIC
> *MASSIVE refactor. Butuh dedicated plan dari Neo sebelum eksekusi.*
> *Jangan dikerjakan ad-hoc. Scope: multi-session, high risk.*
- [ ] **C-1**: Separate domain models dari JPA `@Entity` — buat pure domain class + JPA entity + mapper per bounded context
- [ ] **C-2**: Decouple cross-context JPA FK (`Transactions→Stores`, `Users→Stores`, `Transactions→ProductDenoms`) → UUID-based references

### 🔧 Technical Debt — Code Hygiene (L-series + M-2, M-7, M-9)
> *Nice-to-have. Pick ketika lagi refactor area terkait.*
- [ ] **L-1**: Tambah test coverage — catalog, onboarding, controllers masih 0 test
- [ ] **L-2**: `UserDomainServiceTest` — mock port interfaces, bukan concrete adapters
- [ ] **L-3**: Hapus deprecated `changePassword` di `UserDomainService` (sudah ada di `IdentityDomainService`)
- [ ] **L-4**: `Stores.java` — rename `createdDate/updatedDate` → `createdAt/updatedAt` (konsisten)
- [ ] **L-5**: Hapus dead commented-out code di `KeycloakLoginEventListener.java:31,41`
- [ ] **L-6**: `DataSeeder` — buat idempotent untuk partial runs
- [ ] **L-7**: `BalanceDomainService` — konsistenkan exception types (`ResourceNotFoundException`)
- [ ] **L-8**: Pagination untuk product listing (low urgency, tunggu >100 produk)
- [ ] **L-9**: `KeycloakLoginEventListener` — hapus auto-create Store (legacy dari sebelum onboarding flow)
- [ ] **M-2**: `StoreMutations.java` — tambah `@Version` (append-only tapi inkonsisten dengan pattern)
- [ ] **M-7**: `ProductDenoms.metadata` `@Transient` — null di semua path kecuali `getDenomWithMeta()`
- [ ] **M-9**: `StoreMutationJpaRepository.findTopBy...` — terima `UUID` bukan `Stores` entity

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
- ✅ Database design brainstorming (Hybrid Approach selected)
- ✅ Entities created: Categories, Products, ProductDenoms, ProductDenomMeta
- ✅ Enums created: CategoryType, DenomType
- ✅ Repositories with caching & filtering
- ✅ Migrated @GenericGenerator → @UuidGenerator (modern Hibernate pattern)
- ✅ Documentation: ROADMAP.md, CLAUDE.md

### Week 1: Browse Products (2026-02-12)
- ✅ Service Layer (CategoryService, ProductService, ProductDenomService)
- ✅ DTO mapping (CategoryDTO, ProductDTO, ProductDenomDTO)
- ✅ REST API (ProductCatalogController) - 5 endpoints with caching
- ✅ UI (ProductPageController + Thymeleaf) - category browsing, product grid, denom listing
- ✅ Responsive layout with Tailwind CSS

### Store Onboarding — Task 1-5 (2026-02-20)
- ✅ Stores entity updated (keycloakOrganizationId, phone, LocalDateTime migration)
- ✅ Keycloak Organization API (createOrganization, addMember, createResellerUser)
- ✅ StoreOnboardingInterceptor + WebMvcConfig
- ✅ StoreOnboardingService + AdminOnboardingService
- ✅ OnboardingController + reseller-form.html
- ✅ Sidebar cleanup: hapus menu tanpa controller (/users, /groups, /user-groups, /transactions, /deposit, /settings)

### Self-Service Password Change (2026-02-25)
- ✅ `ChangeMyPasswordRequestDTO` (Bean Validation: newPassword, confirmPassword)
- ✅ `ManageMyProfileUseCase` (port in) + `UserSelfServiceDomainService` (domain service)
- ✅ `UserSelfServiceController` — dual auth principal (OidcUser + Jwt), session-aware
- ✅ `ProfileController` (GET /profile, POST /profile/change-password) — secured
- ✅ `profile.html` Thymeleaf UI — user info card + password change form + toggle show/hide password
- ✅ Build verification: `mvn clean package -DskipTests=true` → **BUILD SUCCESS**

### Code Cleanup & Refinement (2026-02-25)
> *Hapus dead code, simplify password flow, IDE cleanup*
- ✅ **Role Attributes dihapus** — `role-attributes.html`, endpoint API, service logic, port/use case methods (~400 baris dibuang)
- ✅ **Password Change disederhanakan** — `oldPassword` field dihapus (user sudah authenticated via session, tidak perlu ROPC verification)
- ✅ **Profile UI enhanced** — toggle show/hide password (eye icon) untuk newPassword & confirmPassword
- ✅ **IDE files cleanup** — `.idea/.gitignore`, `omnip-services.iml` dihapus dari tracking

### Purchase Prepaid — Task 12-15b (2026-02-24)
- ✅ **Task 12**: Entity `Transactions` + `StoreMutations` (Double-Entry Ledger) + 3 enums
- ✅ **Task 13**: `Stores.balance` (cache) + `BalanceService` (pessimistic lock + ledger)
- ✅ **Task 14**: `ProviderService` interface + `MockProviderService` (90% success, 500ms delay)
- ✅ **Task 15**: `TransactionService` (saga: deduct → provider → refund) + `TransactionController` (purchase, topup, balance)
- ✅ **Task 15b**: `TransactionDTO` + `GET /{id}` + `GET /history` endpoints
- ✅ Arsitektur: **Double-Entry Ledger** (buku tabungan) — `StoreMutations` = source of truth, `Stores.balance` = read cache
- ✅ Polymorphic reference (`referenceType` + `referenceId`) untuk fleksibilitas mutasi

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

**2026-02-25 (FINAL SESSION)** (August — Port Boundary + Config Consolidation Sprint Complete!):
- **H-series COMPLETE** (3 items, 1 session):
  - H-1: Shared package cleaned — `Beans`, `DataSeeder`, `StoreOnboardingInterceptor` now inject port interfaces, not JPA repos
  - H-3: Domain boundary enforced — `GroupMemberInfo` domain record replaces `UserRepresentation` at Keycloak port boundary
  - H-6: Entity leak fixed — `UserDTO.storeId` (UUID) replaces `Stores` entity reference; updated in `KeycloakLoginEventListener`, `UserManagementHelper`, `TransactionController`, `PurchaseFlowIntegrationTest`
- **M-series CONFIG COMPLETE** (4 items, 1 session):
  - M-3: Dual locking documented — pessimistic (financial) + optimistic `@Version` (general concurrency)
  - M-5: Security formalized — `OmniConstants` holds 7 permission constants (`PERM_VIEW_USERS`, `PERM_MANAGE_USERS`, etc.) + role prefixes; all 4 controllers updated to use constants
  - M-6: Virtual threads cleanup — removed irrelevant `tomcat.max-threads` & `min-spare-threads`
  - M-10: Config externalized — `.env` now single source for secrets (DB, Keycloak); `application-secret.yml` deleted; `.gitignore` updated to exclude `.env`, `logs/`, `.claude/settings.local.json`
- **Deployment workflow simplified** — `application.yml` now references `${KEYCLOAK_REALM}`, `${KEYCLOAK_BASE_URL}`, `${DB_URL}` from `.env`
- **Tests**: 15/15 pass (3 integration + 7 unit + 5 others)
- **Merge to main**: Squash merge `user-management-ui` → `main` (commit d187b65), branch deleted (local + remote)
- **9 commits authored** in this session, consolidated into 1 squash commit on main

**2026-02-25** (August — Technical Debt Register → Task Board):
- **Neo audit** → 24 debt items teridentifikasi, 7 sudah di-fix hari ini (sesi sebelumnya + sesi ini).
- **3 fix hari ini**: H-2 (port boundary `UserSessionControllerAdvice`), H-4 (cross-context `RegistrationDomainService`), H-5 (entity leak `TransactionController` → `TransactionSummary` domain record).
- **Sisa 17 items** dimasukkan ke BACKLOG, dikelompokkan per priority:
  - ⭐ HIGH: H-1, H-3, H-6 (port boundary cleanup)
  - MEDIUM: M-1, M-4, M-8 (correctness quick wins) + M-3, M-5, M-6, M-10 (config)
  - 🔴 EPIC: C-1, C-2 (domain model separation — butuh Neo design plan)
  - LOW: L-series + M-2, M-7, M-9 (code hygiene)
- **Rekomendasi**: Jangan sentuh C-1/C-2 tanpa dedicated plan. H-series bisa di-pick kapan saja.

**2026-02-25** (August — Hexagonal Dependency Rules Enforced! ✅):
- **Domain → Adapter dependency ELIMINATED** — Zero `adapter.out` imports in ALL domain services across 4 bounded contexts (catalog, identity, onboarding, transaction).
- **10 domain services fixed**: All now inject port interfaces (`*RepositoryPort`, `*Port`) instead of JPA repos or Keycloak adapter classes.
- **Port interfaces extended**: Added `findById()`, `save()`, `findByEmail()` to ports so domain services can call them without referencing JPA/adapter types.
- **Cross-context violations fixed**: Onboarding services no longer import `identity.adapter.out` — use `OnboardingUserPort` and `KeycloakOrganizationPort` instead.
- **Tests updated**: `TransactionDomainServiceTest` mocks port interfaces. `PurchaseFlowIntegrationTest` uses JPA repo mocks (covers all interfaces) with port-typed aliases for compile-time safety.
- **`mvn clean package` BUILD SUCCESS** — 15 tests pass, 0 failures.
- 5 commits on `user-management-ui` branch.

**2026-02-25** (August — Integration Test Purchase Flow Done! Task 17 ✅):
- **PurchaseFlowIntegrationTest SELESAI** — 3 skenario integration test lulus semua:
  - Happy path: SUCCESS response, provider dipanggil 1x, saldo terpotong
  - Saldo tidak cukup: HTTP 422 INSUFFICIENT_BALANCE, provider tidak dipanggil
  - Provider gagal: status REFUNDED, BalanceDomainService addBalance dipanggil 1x (refund)
- **Tech discovery**: Spring Boot 4.x menghapus `@MockBean` & `@AutoConfigureMockMvc`. Migrasi ke `@MockitoBean` (Spring Framework 7.x) dan `MockMvcBuilders.webAppContextSetup()`.
- **Fix UserSelfServiceDomainServiceTest**: Removed stale `setOldPassword()` calls (oldPassword field dihapus saat Code Cleanup sebelumnya).
- Added `spring-security-test` dependency untuk `jwt()` MockMvc support.
- **Total: 15 tests pass** (3 integration + 7 unit transaction + 3 identity + 2 profile).
- **MVP Sprint COMPLETE** 🎉 — semua critical path tasks selesai.

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

**Last Updated**: 2026-02-25 (Session 5 — Technical Debt H & M series COMPLETE)
**Status**:
- ✅ MVP Sprint COMPLETE
- ✅ H-series (port boundary) DONE
- ✅ M-series (config) DONE
- ⏳ C-series (domain model separation) — Deferred, awaiting Neo plan
- 📌 L-series (code hygiene) — Backlog

**Next Options**:
1. Pick next feature dari BACKLOG (admin org, balance top-up, product management)
2. Tackle C-1/C-2 (require dedicated Neo architecture plan)
3. Implement L-series (test coverage, code cleanup)

