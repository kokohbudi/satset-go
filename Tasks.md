# SatSetGo - Task Board

> **Owner**: August (Senior PM)
> **Last Updated**: 2026-03-03
> **Sprint**: MR-series (Mandatory Role Assignment) — Ready

---

## 🔥 CURRENT SPRINT: MVP Critical Path

> **Prinsip**: Fitur yang membuat user bisa **beli pulsa** = MVP. Sisanya backlog.

### ~~Task 17 — Integration Test Purchase Flow~~ ✅ DONE
- ✅ **Test happy path**: pilih produk → beli → saldo terpotong → transaksi SUCCESS
- ✅ **Test saldo tidak cukup**: error message (HTTP 422), saldo tidak berubah, provider tidak dipanggil
- ✅ **Test provider gagal**: transaksi REFUNDED → saldo dikembalikan otomatis

---

## 📦 BACKLOG (GOOD IDEA, NOT NOW)

### ~~JWT Org ID — OJ-series~~ ✅ CLOSED (Simplified)
> *Keputusan (2026-03-03, Neo review): org_id di JWT TIDAK diperlukan. DB sudah punya chain User → Store → keycloak_organization_id. Single-service architecture = DB lookup cukup.*
> *Cleanup: `org_id` mapper dihapus dari scope `roles`. `group membership` mapper + `organization` scope tetap dipertahankan.*

- [x] **OJ-2**: Keycloak setup — `group membership` mapper + `organization` scope → default ✅
- [~] **OJ-1**: ~~`org_id` protocol mapper~~ — REVERTED (mapper dihapus, tidak diperlukan)
- [~] **OJ-3**: ~~Backend set `org_id` attribute~~ — CANCELLED (DB lookup cukup)
- [~] **OJ-4**: ~~Parse `org_id` dari JWT~~ — CANCELLED (DB lookup cukup)
- [~] **OJ-5**: ~~End-to-end verify~~ — CANCELLED (scope reduced)

### Admin Organization Management (Post-MVP)
> *Dipindahkan dari "UP NEXT" — bukan MVP blocker*
- [-] Task 7-11: KC Org API read/update, AdminOrgService, Admin Org UI, members modal, edit business data
- [-] User segregation backoffice vs reseller (brainstorm Option A/B/C sudah documented)

### ~~Mandatory Role Assignment — MR-series~~ ✅ DONE
> *Neo design plan selesai 2026-03-03. Lihat `TechSpecs.md` → "Mandatory Role Assignment — Technical Blueprint".*
> *Path A (self-service) & Path B (admin create org): sudah auto-assign `org_owner` ✅. Scope hanya Path C (admin create backoffice user).*
> *3 file, ~26 LOC. Sequential: MR-1 → MR-2 → MR-3 → verify.*

- [x] **MR-1**: `CreateUserRequest.java` — tambah `@NotEmpty` validasi pada field `roles` + update comment ✅
- [x] **MR-2**: `IdentityDomainService.java` — fix crash `.getFirst()` pada empty list + support multi-role assignment ✅
- [x] **MR-3**: `user-management.html` — hapus tombol "Skip", enforce role selection mandatory di form ✅

### Admin User Management Cleanup (Post-MVP)
- [-] Fix `/admin/user-management` — filter hanya tampilkan backoffice users
- [-] Sidebar: menu `/users`, `/groups` sudah dihapus (tidak ada page controller)
- [-] **Refactor User Search** — migrasi dari Keycloak Admin API ke DB lokal untuk database-level pagination. Saat ini fetch all → filter di memory, tidak efisien. Target: query langsung ke DB, lazy-load roles hanya untuk user yang tampil.

### Wallet Refactor — WR-series (Preparation, bisa dikerjakan sekarang)

> **Goal**: Bersihkan domain model debit/kredit di Core — hapus cross-context coupling, pisahkan balance ke entity sendiri.
> **Prerequisite W-series**: WR-series harus selesai dulu sebelum Wallet extraction.
> **Tidak butuh keputusan repo** — ini semua di `omnip-services-3`.
> **Risk**: Medium (DB migration `wallet_accounts`), zero functional change.
> Design lengkap: `TechSpecs.md` → "Wallet Refactor — WR-series"

- [x] **WR-1**: Buat `WalletAccount.java` entity — `{ storeId (UUID), balance, version }`, table `wallet_accounts` ✅
- [x] **WR-2**: Buat `WalletAccountPort.java` + `WalletAccountJpaRepository.java` — `findByStoreId`, `findByStoreIdWithLock` (pessimistic), `save` ✅
- [x] **WR-3**: Buat `MutationResult.java` domain record — `{ mutationId, balanceAfter }` — clean return type untuk port ✅
- [x] **WR-4**: Fix `StoreMutations.java` — hapus `@ManyToOne Stores store`, ganti `UUID storeId` + tambah idempotency `@UniqueConstraint` ✅
- [x] **WR-5**: Fix `BalanceManagementUseCase.java` — return type `StoreMutations` → `MutationResult` ✅
- [x] **WR-6**: Refactor `BalanceDomainService.java` — inject `WalletAccountPort` (ganti `StoreBalancePort`), update logic ✅
- [ ] **WR-7**: Fix `TransactionDomainService.java` — inject `BalanceManagementUseCase` (port, bukan concrete), hapus `StoreBalancePort`
- [ ] **WR-8**: `Stores.java` — `@Deprecated balance` field (nullable, bukan source of truth lagi)
- [ ] **WR-9**: Delete `StoreBalancePort.java` — setelah verify zero consumer (`grep -r "StoreBalancePort"`)
- [ ] **WR-10**: `DataSeeder.java` — seed `WalletAccount` dari existing `Stores.balance` (idempotent)
- [ ] **WR-11**: Fix `TransactionDomainServiceTest.java` — mock `BalanceManagementUseCase` port
- [ ] **WR-12**: Fix `PurchaseFlowIntegrationTest.java` — mock `WalletAccountPort`, hapus `StoreBalancePort` mocks
- [ ] **WR-VERIFY**: `mvn clean package` → BUILD SUCCESS, semua tests pass

### Wallet Service Separation — W-series (EPIC, Plan dulu)

> **Context**: Pisahkan urusan finansial (balance, mutasi, top-up, refund) ke service terpisah `omnip-wallet`.
> Auth: Keycloak shared realm `satset-go`. Communication: REST (internal API). Saga pattern untuk distributed tx.
> Design lengkap: lihat `TechSpecs.md` → "Wallet Service — Technical Design"
> **BLOCKER sebelum eksekusi**: Jawab Open Questions (repo structure + DB strategy)

- [x] **W-0**: Keputusan repo structure → ✅ **Multi-module Maven** dalam 1 repo. DB: schema terpisah di DB yang sama.

> **Prerequisite W-series**: WR-series harus selesai dulu.
> Design lengkap: `TechSpecs.md` → "Multi-Module Maven — Structure & Migration Plan"

- [ ] **W-SETUP**: Restructure project → multi-module Maven
  - Buat `omnip-core/` + `git mv src omnip-core/src`
  - Update root `pom.xml` → parent POM (`packaging: pom`, tambah `<modules>`)
  - Buat `omnip-core/pom.xml` (inherit parent, deps dari root pom lama)
  - Verify: `cd omnip-core && mvn compile` → BUILD SUCCESS
- [ ] **W-1**: Buat `omnip-wallet/` module skeleton — Spring Boot app, Keycloak Resource Server, `/actuator/health`
- [ ] **W-2**: Wallet domain model — `WalletAccount`, `WalletMutation` entities (pindah dari Core setelah WR-series)
- [ ] **W-3**: Wallet use cases + domain service — `debit`, `credit`, `refund`, `getBalance`, `getMutationHistory`
- [ ] **W-4**: Wallet REST endpoints — `/internal/wallet/*` (5 endpoints per API contract di TechSpecs)
- [ ] **W-5**: Core: `WalletClient` (RestClient) — call Wallet API, service account auth (client credentials)
- [ ] **W-6**: Core: replace `BalanceDomainService` calls → `WalletClient` di `TransactionDomainService`
- [ ] **W-7**: Data migration — seed `wallet_accounts` + `wallet_mutations` dari Core DB
- [ ] **W-8**: Integration test end-to-end — purchase flow Saga (Core → Wallet debit → Provider → Wallet refund)

### Balance Top-up (Week 3+, koordinasi dengan W-series)
- [-] MockPaymentGateway implementation — **akan masuk ke Wallet Service (bukan Core)**
- [-] Top-up UI di Core — call Wallet credit endpoint
- [-] **Store Mutations UI**: Tab riwayat mutasi saldo di menu /transactions — fetch dari Wallet API

### ~~Admin Product Management — AP-series (Week 4)~~ ✅ DONE
> *Neo design plan selesai 2026-03-02. Lihat `TechSpecs.md` → "Admin Product Management Blueprint" untuk detail.*
> *Session A = backend (AP-1..AP-7). Session B = frontend (AP-8..AP-11).*
> *Bonus: Hex architecture fix (request DTOs → domain layer) + 27 unit tests + Keycloak roles setup.*

**Session A — Backend:**
- [x] **AP-1**: Extend port out interfaces — `findById`, `findAllAdmin`, `existsByCodeAndIdNot` untuk Category/Product/Denom ✅
- [x] **AP-2**: Verify JPA adapters compile setelah AP-1 (JpaRepository auto-satisfy) ✅
- [x] **AP-3**: Buat 3 use case interfaces + 6 request DTO records (Create/Update per entity) ✅
- [x] **AP-4**: Extend `CategoryDomainService` — implements `ManageCategoriesUseCase` + `@CacheEvict` ✅
- [x] **AP-5**: Extend `ProductDomainService` — implements `ManageProductsUseCase` + cascade softDelete denoms ✅
- [x] **AP-6**: Extend `DenomDomainService` — implements `ManageDenomsUseCase` ✅
- [x] **AP-7**: `OmniConstants` (2 constants: `PERM_VIEW_CATALOG`, `PERM_MANAGE_CATALOG`) + `SecurityConfig` path rules ✅

**Session B — Frontend:**
- [x] **AP-8**: `AdminCatalogController` — Category CRUD REST endpoints ✅
- [x] **AP-9**: `AdminCatalogController` — Product + Denom CRUD REST endpoints ✅
- [x] **AP-10**: `AdminCatalogPageController` + 3 Thymeleaf templates (categories, products, denoms) ✅
- [x] **AP-11**: Sidebar link + `mvn clean package` verify ✅

**Bonus (sesi yang sama):**
- [x] **AP-HEX**: Hex fix — request DTOs dipindah dari `adapter.in.web.dto` → `domain.port.in` ✅
- [x] **AP-TEST**: 3 test classes, 27 unit tests (Category 8, Product 10, Denom 9) ✅
- [x] **AP-KC**: Keycloak roles `view_catalog` + `manage_catalog` created & assigned to admin@satset-go.id ✅

### ~~Catalog Drill-down Navigation — AP-N series~~ ✅ DONE
> *Implemented bersamaan dengan AP-series (2026-03-02). Detail di `TechSpecs.md` → "Catalog Drill-down Navigation — Action Plan (Option A)".*
> *Single sidebar "Kelola Katalog" → Categories → Products (filtered) → Denoms. 4 file, ~36 LOC.*

- [x] **AP-N1**: `AdminCatalogPageController` — root redirect `/admin/catalog` + `@RequestParam` categoryId/categoryName di `productsPage()` ✅
- [x] **AP-N2**: `categories.html` — tambah tombol "Produk →" per row di kolom Aksi ✅
- [x] **AP-N3**: `products.html` — breadcrumb conditional + JS `INITIAL_CATEGORY_ID` + pre-set `filterCategoryId` ✅
- [x] **AP-N4**: `denoms.html` — extend `loadProduct()` untuk category context + fix breadcrumb dynamic links ✅
- [x] **AP-N5**: Manual test drill-down end-to-end + `mvn compile` verify ✅

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
> *C-2 selesai 2026-03-06 (dikerjakan oleh Qwen, diverifikasi Claude). 339/339 tests pass.*
- [ ] **C-1**: Separate domain models dari JPA `@Entity` — buat pure domain class + JPA entity + mapper per bounded context
- [x] **C-2**: Decouple cross-context JPA FK (`Transactions→Stores`, `Users→Stores`, `Transactions→ProductDenoms`) → UUID-based references ✅

### 🔧 Technical Debt — Code Hygiene (L-series + M-2, M-7, M-9)
> *Nice-to-have. Pick ketika lagi refactor area terkait.*
- [x] **L-1**: Tambah test coverage — KeycloakLoginEventListenerTest (10 tests, 316 LOC) ✅ 2026-03-06
- [x] **L-2**: `UserDomainServiceTest` — mock port interfaces (`UserRepositoryPort`, `KeycloakIdentityPort`) ✅
- [x] **L-3**: Hapus deprecated methods di `UserDomainService` (`setUserStatus(UserDTO)`, `createNewUser(UserDTO)`) ✅
- [x] **L-4**: `Stores.java` — rename `createdDate/updatedDate` → `createdAt/updatedAt` (konsisten) ✅
- [x] **L-5**: Hapus dead commented-out code di `KeycloakLoginEventListener.java:31,41` ✅
- [x] **L-6**: `DataSeeder` — idempotent: `findByCode().orElseGet()` per item + count() fast-path guard ✅
- [x] **L-7**: `BalanceDomainService` — `RuntimeException` → `ResourceNotFoundException` (3 occurrences) ✅
- [ ] **L-8**: Pagination untuk product listing (low urgency, tunggu >100 produk)
- [x] **L-9**: `KeycloakLoginEventListener` — auto-create Store sudah tidak ada (removed in previous session) ✅
- [x] **M-2**: `StoreMutations.java` — tambah `@Version` (append-only tapi inkonsisten dengan pattern) ✅
- [x] **M-7**: `ProductDenoms.metadata` `@Transient` — tambah komentar documenting intentional null-by-default behavior ✅
- [x] **M-9**: `StoreMutationJpaRepository.findTopBy...` — terima `UUID` bukan `Stores` entity ✅

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

**2026-03-02** (August — AP-series Admin Product Management COMPLETE! ✅):
- **AP-1..AP-7 (Backend) SELESAI**:
  - Port out interfaces extended (findById, findAllAdmin, existsByCodeAndIdNot)
  - 3 use case interfaces: `ManageCategoriesUseCase`, `ManageProductsUseCase`, `ManageDenomsUseCase`
  - 6 request DTO records (Create/Update per entity) di domain layer
  - 3 domain services extended dengan CRUD + `@CacheEvict`
  - `OmniConstants` + `SecurityConfig` path rules
- **AP-8..AP-11 (Frontend) SELESAI**:
  - `AdminCatalogController` — full CRUD REST endpoints (Category + Product + Denom)
  - `AdminCatalogPageController` + 3 Thymeleaf templates
  - Sidebar link "Kelola Katalog" via Keycloak role attribute
- **Bonus**:
  - Hex architecture fix: request DTOs moved dari `adapter.in.web.dto` → `domain.port.in`
  - 27 unit tests (3 classes: Category 8, Product 10, Denom 9)
  - Keycloak roles `view_catalog` + `manage_catalog` created & assigned
- **UX Decision**: Single sidebar menu "Kelola Katalog" → drill-down (Categories → Products → Denoms)
- ~~**Bug Report**: Tombol "Add User" tidak muncul di `/admin/user-management` meski role `manage_users` sudah ada~~ → ✅ Solved
- **⚠️ Uncommitted**: Semua perubahan masih di branch `admin-product-management`, belum commit

**2026-03-01** (August — L-series & M-series Code Hygiene Sprint):
- **M-2**: `StoreMutations.java` — `@Version` ditambah ✅
- **M-9**: `StoreMutationJpaRepository.findTopBy...` — parameter `Stores` → `UUID` ✅
- **L-4**: `Stores.java` — `createdDate/updatedDate` → `createdAt/updatedAt` + `@Column(name=...)` untuk DB compat ✅
- **L-5**: Dead commented-out code dihapus dari `KeycloakLoginEventListener` (line 31, 41) ✅
- **L-2**: `UserDomainServiceTest` — mocks diupdate: `UserJpaRepository` → `UserRepositoryPort`, `KeycloakAdminClientService` → `KeycloakIdentityPort` ✅
- **L-3**: Deprecated methods dihapus dari `UserDomainService`: `setUserStatus(UserDTO)` + `createNewUser(UserDTO)` ✅
- **L-6**: `DataSeeder` idempotent — `findByCode().orElseGet()` per item + `count()` fast-path guard ✅
- **L-7**: `BalanceDomainService` — 3x `RuntimeException` → `ResourceNotFoundException` ✅
- **L-9**: Dikonfirmasi sudah done di sesi sebelumnya — tidak ada auto-create Store di listener ✅
- **M-7**: `ProductDenoms.metadata @Transient` — komentar ditambah untuk mendokumentasikan intentional null-by-default ✅
- **Tests**: 14/14 pass (berkurang 1 karena test deprecated `shouldSetUserStatus` dihapus bersama L-3)
- **Sync**: Tasks.md + Google Tasks updated setiap task selesai

---

**2026-03-05** (Session 8 — Unit Test Coverage Sprint COMPLETE! ✅):
- **JaCoCo Coverage: 68% → 94%** 🎉
  - Fixed 2 JaCoCo exclude path bugs (DataSeeder, KeycloakLoginEventListener)
  - Added OmniConstants + LogoutController to excludes
  - Instruction coverage: **94%**, Branch coverage: **72%**
- **Test Suite Expansion**: 286 tests → **320 tests** (+34 tests)
  - `AdminCatalogControllerTest`: 8 new tests (create/update category, product, denom + listDenoms, getDenom found)
  - `CategoryDomainServiceTest`: 8 new read/browse tests (findAll, findByCode×3, findByType, findAllForAdmin, findById×2)
  - `ProductDomainServiceTest`: 8 new read/browse tests (findByCategory×2, findActiveProducts, findByCode×2, findByCategoryForAdmin, findById×2)
  - `DenomDomainServiceTest`: 8 new read/browse tests (findByProduct×2, findByCode×2, getDenomWithMeta×2, findByProductForAdmin, findById×2)
  - `TransactionDomainServiceTest`: 10 new tests (createPurchase store/denom not found, topUp×3, getTransaction×2, getTransactionHistory×2)
- **⚠️ Note**: Keycloak adapter testing (KeycloakAdminClientService, KeycloakLoginEventListener) **NOT covered** — requires live Keycloak infrastructure. **Plan**: Test containers (Testcontainers) untuk session mendatang
- **JPA Repository interfaces**: Expected 0% coverage (Spring Data JPA generates implementations at runtime, not compile-time)
- **BUILD SUCCESS**: `mvn test` 320/320 pass ✅

**Last Updated**: 2026-03-06 (Session 11 — WR-1..WR-6 Wallet Refactor)
**Status**:
- ✅ MVP Sprint COMPLETE
- ✅ H-series (port boundary) DONE
- ✅ M-series (config + correctness + hygiene) DONE
- ✅ L-series (code hygiene) MOSTLY DONE — ✅ L-1 (test coverage) KeycloakLoginEventListener 10 tests DONE, ⏳ L-8 (pagination, low urgency)
- ✅ AP-series (admin product management) DONE — backend + frontend + tests + Keycloak roles
- ✅ AP-N series (catalog drill-down) DONE
- ✅ Unit test coverage (repository-based, mocked ports) — **94% instruction**
- ✅ KeycloakLoginEventListenerTest — 10 tests added (OIDC login flow, role extraction, user registration)
- ✅ C-2 DONE — cross-context JPA FK decoupled → UUID references (339/339 tests pass)
- ✅ WR-1..WR-6 DONE — WalletAccount entity, WalletAccountPort, MutationResult, BalanceDomainService refactored (349/349 tests pass)
- ⏳ C-1 (separate JPA entity from domain model) — next step C-series
- ⏳ WR-7..WR-12 — TransactionDomainService, Stores.balance deprecate, StoreBalancePort delete, DataSeeder, test fixes

**2026-03-06** (August — C-2 Domain Model Decoupling COMPLETE! ✅):
- **C-2 DONE** — All cross-context JPA `@ManyToOne` FK references replaced with plain `UUID` fields
- 3 entities cleaned: `Transactions` (removed `Stores` + `ProductDenoms` FK), `StoreMutations` (removed `Stores` FK), `Users` (removed `Stores` FK)
- Snapshot fields `denomName` + `productName` added to `Transactions` — correct DDD: transaction records what was purchased at creation time
- 14 main files updated, all consumers migrated (`getStores()` → `getStoreId()`, `setStores()` → `setStoreId()`)
- JPQL queries in `TransactionJpaRepository` + `UserJpaRepository` updated (no more cross-context JOIN FETCH)
- **Build**: `mvn test` → **339/339 PASS**, BUILD SUCCESS
- **Executed by**: Qwen (code agent), **Verified by**: Claude
- **Unlocks**: WR-series (WR-4 already covered), C-1 can now proceed with cleaner entity boundaries

**2026-03-06** (August — KeycloakLoginEventListenerTest Added! ✅):
- **L-1 COMPLETE** — `KeycloakLoginEventListenerTest.java` created with 10 unit tests
- **Test coverage**: Main login flow (new user + existing user), role extraction (realm + client roles), provider user ID extraction, email/username/fullname extraction from OIDC attributes
- **Technical approach**: Use reflection to inject @Value clientId field in test setup; mock OAuth2LoginAuthenticationToken, OidcUser, JwtDecoder
- **Test count**: 320 → 330 tests (+10)
- **Files**: 1 new test file (316 LOC)
- **Branch**: `test-coverage-gaps` created, 1 commit

**2026-03-05** (Session 8 — Unit Test Coverage Sprint COMPLETE! ✅):
- **Custom MCP Keycloak** dibangun (49 tools) — manage realm, users, roles, composites, groups, client scopes, protocol mappers. File: `~/myProjects/mcp-keycloak/index.js`
- **MCP PostgreSQL** ditambahkan — read-only query ke `omni_pulsa`
- **OJ-2 DONE**: `group membership` mapper + scope `organization` → default
- **OJ-1 REVERTED**: `org_id` mapper dihapus — tidak diperlukan
- **OJ-3..OJ-5 CANCELLED**: Neo review: DB chain `User → Store → keycloak_organization_id` cukup untuk single-service. JWT org_id hanya diperlukan kalau scale ke microservices.
- **Keycloak Org tetap dipakai**: Member management, domain-based auto-join, SSO isolation. Tapi data filtering tetap lewat DB.

**Next Options**:
1. **🔜 AP-N series**: Catalog drill-down navigation (~55 mnt, 1 sesi)
2. Pick next feature dari BACKLOG (balance top-up)
3. Tackle C-1/C-2 (require dedicated Neo architecture plan)
4. L-1: Tambah test coverage (catalog, onboarding, controllers)

