# Julia.md - SatSetGo Product Intelligence

> **Owner**: Julia (Senior BA & Product Strategist)
> **Last Updated**: 2026-02-20
> **Sprint**: Store Onboarding (Prerequisite Week 2)

---

## Project Vision

**SatSetGo** adalah platform Multi-tenant SaaS Server Pulsa & PPOB yang mengutamakan **kecepatan transaksi** ("sat-set") dan **skalabilitas bisnis** melalui model reseller berjenjang.

### Value Proposition
| Stakeholder | Value |
|---|---|
| **End User** | Beli pulsa/PPOB cepat, harga kompetitif |
| **Reseller (Store)** | Markup sendiri, sistem upline-downline, passive income |
| **Platform (SatSetGo)** | Margin dari selisih base price & sell price, volume-driven |

### Business Model
```
Supplier (H2H) → SatSetGo (base price + margin) → Store/Reseller (markup) → End User
```

### Key Metrics (Target)
- **Transaction Success Rate**: >95%
- **Avg Transaction Latency**: <3 detik
- **Monthly Active Stores**: TBD (belum ada data)
- **Revenue per Transaction**: TBD (margin belum di-define)

---

## Current Sprint/Focus

### Prerequisite: Store Onboarding + Keycloak Organization

**Objective**: Setiap user harus punya Store (toko) yang terdaftar sebagai Organization di Keycloak sebelum bisa bertransaksi. Dua jalur pendaftaran: self-service dan admin-created.

**Why This First (Before Week 2)**:
- Transaksi dilakukan atas nama Store, bukan User
- Balance nanti di level Store (keputusan Julia sebelumnya)
- Multi-user per Store perlu Organization untuk role isolation
- Admin toko A tidak boleh lihat data toko B (multi-tenancy)

**Business Requirements**:

#### Path A — Self-service (Google Login / Register)
- [ ] User login pertama kali → belum punya Store → redirect ke halaman "Daftarkan Toko"
- [ ] Form minimal: **Nama Toko**, **No. HP** (opsional: alamat)
- [ ] Bisa selesai dalam 30 detik — minimal friction
- [ ] Tidak ada approval process — toko langsung aktif
- [ ] Backend: Create Store (DB) + Create Organization (Keycloak) + Assign user ke Organization
- [ ] Setelah onboarding selesai → redirect ke Dashboard

#### Path B — Admin-created
- [ ] Admin → User Management → "Tambah Reseller Baru"
- [ ] Form: Username, Email, Nama Toko, Role, Upline (opsional)
- [ ] Backend: Create User (Keycloak) + Create Store (DB) + Create Organization (Keycloak) + Assign
- [ ] User terima email / bisa langsung login → sudah punya toko

#### Keycloak Organization Sync (Bi-directional)
- [ ] Store dibuat di app → Organization otomatis dibuat di Keycloak
- [ ] Organization dibuat di Keycloak → bisa di-link ke Store di app
- [ ] User di-assign ke Organization → muncul sebagai member Store
- [ ] Store entity perlu field baru: `keycloakOrganizationId` (UUID)

**Key Decisions**:

| Pertanyaan | Keputusan | Rationale |
|---|---|---|
| Store = Organization? | **1 Store = 1 Organization** | Simplest mapping, satu toko = satu tenant |
| Onboarding model | **Guided + Admin** | 2 jalur: reseller mandiri (form) + admin-created |
| Approval process | **Tidak ada** | Toko langsung aktif, kurangi friction |
| Form fields | **Minimal (2-3 field)** | Nama Toko + No. HP. Bisa dilengkapi nanti |
| **Onboarding trigger** | **Spring Interceptor** | Lebih robust dari EventListener — handle kasus user tutup tab, cek di setiap request, flag `hasStore` di-cache di session |
| **Password (Path B admin-created)** | **Keycloak `UPDATE_PASSWORD` required action** | User terima email set-password dari Keycloak, lebih aman, tidak perlu admin share password manual |

**Technical Dependencies**:
- [x] ~~Stores entity: tambah `keycloakOrganizationId`, `phone`, migrate `Date` → `LocalDateTime`~~ → ✅ **DONE Task 1**
- [ ] `KeycloakAdminClientService`: tambah methods Organization API (`createOrganization`, `addMemberToOrganization`) — **API tersedia, library 26.0.8 ✅**
- [ ] Onboarding UI: form Thymeleaf "Daftarkan Toko"
- [ ] **Buat `StoreOnboardingInterceptor`** (Spring `HandlerInterceptor`): intersep setiap request, cek apakah user punya Store. Jika tidak → redirect ke `/onboarding`. Cache flag `hasStore` di `HttpSession` untuk efisiensi
- [ ] Modify `KeycloakLoginEventListener`: **hapus** logika auto-create Store (serahkan ke Interceptor)
- [ ] Path B (Admin): saat create user via admin, set Keycloak required action `UPDATE_PASSWORD` agar user terima email set-password

**Risks**:

| Risk | Impact | Mitigation |
|---|---|---|
| ~~Keycloak Organization API belum ada di admin-client lib~~ | ~~Blocking~~ | ✅ **CLOSED** — `OrganizationsResource` tersedia di 26.0.8 |
| Existing users tanpa Organization | Data inconsistency | Migration script: assign existing Stores ke Organizations |
| User login tapi close tab sebelum isi form toko | Orphan user | ✅ **MITIGATED** — Interceptor akan redirect ulang saat request berikutnya |
| Interceptor overhead | Minor performance | Cache `hasStore` flag di session — hit DB hanya sekali per session |

---

### Mandatory Role Assignment (All User Creation Paths)

**Objective**: Zero user tanpa role di SatSetGo. Setiap user yang masuk ke sistem **pasti** punya minimal 1 role. Tidak boleh ada user "orphan" tanpa role.

**Why**:
- User tanpa role = tidak bisa apa-apa di sistem, tapi tetap occupy resources
- Role menentukan akses (apa yang boleh dilihat/dilakukan)
- Konsistensi data: setiap user punya identitas jelas (Owner, Operator, Admin)

**Keycloak Role Hierarchy** (Client: `satsetgo-client`):
```
org_owner          ← pemilik toko (composite)
  └─ org_operator  ← staff/kasir (composite)
       └─ transaction  ← hak transaksi
```

**Business Rules per Alur:**

#### Path A — Self-service Onboarding (Google Login → Daftar Toko)
- User daftar sendiri → **otomatis dapat `org_owner`** (client role)
- Tidak perlu pilih role, auto-assign saat onboarding selesai (Store created)
- Rationale: yang daftar sendiri = pemilik toko

#### Path B — Admin Create Org/Reseller (backoffice onboarding)
- Admin buat organisasi baru di backoffice → user yang dibuat **otomatis dapat `org_owner`** (client role)
- Sama seperti Path A, auto-assign, bukan pilihan
- Rationale: setiap org punya 1 owner by default

#### Path C — Admin Create User di `/admin/user-management`
- Ini untuk user **backoffice/admin** (bukan reseller)
- Role admin **wajib diinput** (mandatory field di form)
- Role di sini = **realm roles** (view_users, manage_users, view_catalog, manage_catalog, dll.)
- Tidak boleh submit tanpa memilih minimal 1 role

**Key Decisions:**

| Pertanyaan | Keputusan | Rationale |
|---|---|---|
| Role untuk self-service | **Auto `org_owner`** | Owner = yang daftar sendiri |
| Role untuk admin-created org | **Auto `org_owner`** | Setiap org harus punya owner |
| Role untuk admin-created user | **Mandatory input** | Admin harus tentukan role backoffice |
| Existing users tanpa role | **Migration needed** | Cek dan assign retroactively |

**Date**: 2026-03-03

---

### Admin: Organization Management Screen

**Objective**: Admin bisa melihat, mengelola, dan memonitor semua organisasi/toko dalam satu tabel. CRUD org dari sisi admin backoffice.

**Business Requirements**:

#### Tabel List Organisasi
- [ ] Tampilkan semua organisasi dari **Keycloak Organizations API** (primary source)
- [ ] Enrichment data bisnis (phone, email, upline) dari DB `Stores` via `keycloakOrganizationId` join
- [ ] Kolom: Nama Org, Email, Phone, Upline, Status (enabled/disabled), Aksi
- [ ] Search by nama organisasi

#### Enable/Disable Organisasi
- [ ] Toggle `enabled` attribute di Keycloak Organization
- [ ] Konfirmasi dialog sebelum disable
- [ ] Reflect status change langsung di tabel

#### Lihat Members
- [ ] Modal menampilkan semua member dari KC Organization
- [ ] Tampilkan: username/email member
- [ ] Data dari Keycloak Organization Members API

#### Tambah Reseller (Modal)
- [ ] Form modal (bukan redirect ke page terpisah)
- [ ] Fields: Username, Email, Nama Toko, Phone, Role (dropdown), Upline (opsional)
- [ ] Reuse logic dari `AdminOnboardingService.onboardReseller()`
- [ ] Toast notification sukses/gagal

#### Edit Data Bisnis
- [ ] Edit phone, email → simpan ke DB `Stores`
- [ ] Modal form atau inline edit
- [ ] Tidak mengubah data di Keycloak (nama org tetap dari KC)

**Data Source Strategy: Hybrid**

| Data | Source | Operasi |
|------|--------|--------|
| Nama org, enabled, members | Keycloak Organizations API | Read, Toggle |
| Phone, email, referral, upline | DB `Stores` | Read, Edit |
| Tambah reseller | Both | KC: create org+user, DB: create store+user |

**Key Decisions**:

| Pertanyaan | Keputusan | Rationale |
|---|---|---|
| Data source utama | **Keycloak Organizations API** | Source of truth untuk org identity + member |
| Business data storage | **DB Stores** | Phone, email, upline tetap di DB — KC hanya identity |
| Enable/disable mechanism | **KC org `enabled` attribute** | Bukan dari DB `stores.active` |
| Tambah reseller UI | **Modal form** (bukan page terpisah) | UX lebih baik — tidak keluar dari context |
| Scope | **List, toggle, members, add, edit bisnis** | Delete masuk V2 (cascade complex) |

**Technical Dependencies**:
- [ ] `KeycloakAdminClientService`: tambah methods `getOrganizations()`, `getOrganization(id)`, `updateOrganization()`, `getOrganizationMembers()`
- [ ] `AdminOrgPageController`: render halaman + SSR data
- [ ] `AdminOrgController`: REST endpoints (list, toggle status, edit)
- [ ] `StoreRepository`: extend query methods (findByKeycloakOrganizationId)
- [ ] Template: `pages/admin/org-management.html` (DaisyUI + Alpine.js)

---

### Week 2: Purchase Prepaid Flow (After Onboarding)

**Objective**: User bisa beli produk prepaid (pulsa, data) dengan mock provider.

**Business Requirements**:
- [ ] User harus punya saldo sebelum bisa beli
- [ ] Validasi: produk aktif, saldo cukup, denom tersedia
- [ ] Harga = `price` + `adminFee` dari ProductDenoms
- [ ] Status transaksi: PENDING → PROCESSING → SUCCESS / FAILED
- [ ] Jika FAILED, saldo harus dikembalikan (refund otomatis)
- [ ] History transaksi bisa dilihat user

**Technical Dependencies**:
- [ ] Tambah field `balance` (DECIMAL 15,2) di entity `Users`
- [ ] Entity baru: `Transactions`, `TransactionItems`
- [ ] Interface `ProviderService` + `MockProviderService`
- [ ] Balance locking mechanism (pessimistic lock untuk deduct)

**Key Decision Needed**:

| Pertanyaan | Opsi A | Opsi B | Rekomendasi |
|---|---|---|---|
| Balance scope | Per User | Per Store | **Per Store** - karena reseller = store, balance harusnya di level bisnis |
| Pricing model | Fixed margin (dari DB) | Dynamic margin (per store level) | **Fixed dulu** - Week 2 fokus flow, dynamic pricing Week 4+ |
| Refund mechanism | Auto-refund on FAILED | Manual admin approval | **Auto-refund** - UX lebih baik, complexity rendah |

---

## Backlog

> **Note**: Payment/Balance system akan jadi microservice terpisah. Backlog ini khusus SatSetGo core platform.
> **PIC Timeline**: August (PM) — Julia hanya draft, August yang saring & jadwalkan.

### Current Sprint
- [ ] **Store Onboarding + Keycloak Organization** - prerequisite sebelum Week 2 (CURRENT)
- [ ] **Admin Organization Management** - tabel CRUD org (enable/disable, members, tambah reseller modal, edit data bisnis)
- [ ] **Purchase Flow** - transaksi prepaid end-to-end (Week 2, after onboarding)
- [ ] **Admin Product CRUD** - kelola catalog tanpa akses DB (Week 4)

### Revenue & Pricing
- [ ] **Reseller Tier & Dynamic Pricing** - harga berbeda per tier (Bronze→Silver→Gold→Platinum), naik otomatis dari volume transaksi bulanan. *Standar industri — tanpa ini reseller pindah kompetitor.*
- [ ] **Markup per Store** - setiap Store set markup sendiri di atas harga platform. *Core value prop buat reseller — tanpa ini mereka bukan "jualan", cuma beli untuk sendiri.*
- [ ] **Komisi Upline (Rebate System)** - upline dapat komisi per transaksi downline (Rp 25-50/trx). *Network effect driver — reseller jadi sales force gratis. Field `Stores.upline` sudah siap.*

### Reseller Experience
- [ ] **White-label Storefront** - setiap Store punya URL sendiri (`tokopulsa.satsetgo.com` / custom domain). End customer beli dari "toko" reseller. *Competitive moat vs aggregator biasa.*
- [ ] **Dashboard Analytics per Store** - total transaksi, produk terlaris, profit bulanan, performa downline. Chart sederhana + summary cards. *Reseller yang lihat profit-nya = lebih engaged.*
- [ ] **API Key untuk Reseller** - reseller besar bisa integrasi via REST API, bypass UI. *Volume tinggi = revenue stabil. Banyak server pulsa besar survive dari channel ini.*
- [ ] **Bulk/Batch Transaction** - upload CSV / input banyak nomor untuk kirim pulsa massal. Use case: corporate, giveaway. *Niche tapi margin tinggi.*

### Platform & Operations
- [ ] **Auto-switch Supplier (Failover)** - kalau supplier H2H down, otomatis switch ke backup. Prioritas: harga terbaik → success rate → latency. *Single supplier = single point of failure.*
- [ ] **Product Price Watcher** - pantau harga dari multiple supplier, alert admin kalau ada perubahan signifikan. Opsi auto-adjust harga jual. *Harga sering berubah — telat update = margin minus.*
- [ ] **Dispute & Complaint Management** - reseller submit komplain (transaksi sukses tapi pulsa nggak masuk). Admin investigate, eskalasi ke supplier, refund. *Tanpa ini admin handle via WA — nggak scalable.*
- [ ] **Audit Log & Activity Trail** - semua aksi tercatat (login, ubah harga, approve refund). *Partial ada — entity punya createdBy/updatedBy. Perlu expand ke action-level.*

### Product Expansion
- [ ] **Postpaid Inquiry** - cek tagihan PLN/PDAM/Telkom sebelum bayar. *Schema sudah support (`requiresInquiry`, `minAmount/maxAmount`).*
- [ ] **Produk Non-Telco** - voucher game (ML, FF, Genshin), e-money (GoPay, OVO, DANA), streaming (Netflix, Spotify), token listrik. *Categories entity tinggal tambah entry. Game voucher = high margin.*
- [ ] **Real Provider Integration** - Digiflazz / VIP Reseller API. *Swap `MockProviderService` → real implementation.*

### Growth & Engagement
- [ ] **Promo & Voucher Engine** - diskon per produk, cashback volume, voucher code user baru. Rules engine: kondisi → reward. *Acquisition & retention tool.*
- [ ] **Notification Engine** - event-driven: trx sukses/gagal, saldo menipis, harga berubah, downline baru, target tier hampir tercapai. Channel: in-app, email, webhook (WA gateway). *Cocok dengan event-driven architecture.*
- [ ] **Gamification** - badge/level reseller berdasarkan volume. Leaderboard bulanan. *Retention & engagement.*
- [ ] **Referral Tracking** - dashboard performa upline-downline, commission report. *Field `Stores.upline` sudah ada.*

### ~~Long-term Vision~~ (Removed)
- ~~**Multi-currency & Region**~~ — *Dicoret oleh owner. Belum ada rencana go internasional.*

---

## Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| **Store belum link ke Keycloak Org** | Multi-tenancy tidak jalan, data bocor antar toko | HIGH | **Store Onboarding harus selesai sebelum Week 2** |
| **Balance race condition** | Saldo minus, kerugian finansial | HIGH (concurrent users) | Pessimistic locking pada deduct balance |
| **Provider downtime** | Transaksi gagal, user kecewa | MEDIUM | Auto-switching logic (multi-supplier) |
| **No unit tests** | Regression bugs saat refactor | MEDIUM | Week 4 dedicated untuk testing |
| ~~**Stores.createdDate pakai java.util.Date**~~  | Inkonsistensi dengan entity lain (LocalDateTime) | LOW | ✅ **DONE Task 1** |
| **Users belum punya balance** | Blocking untuk Week 2 | HIGH | Harus ditambahkan sebelum mulai purchase flow |
| **No pagination** | Performance issue saat data besar | LOW (early stage) | Tambahkan saat product catalog > 100 items |

---

## Data Insights

### Current Codebase Analysis (2026-02-12)

**Entity Inventory**:
| Entity | Fields | Status | Notes |
|---|---|---|---|
| Categories | 11 fields | Production ready | Enum: PREPAID/POSTPAID |
| Products | 12 fields | Production ready | ManyToOne → Categories |
| ProductDenoms | 20 fields | Production ready | Pricing: nominal, price, basePrice, adminFee |
| ProductDenomMeta | - | Production ready | Key-value flexibility |
| Users | 12 fields | **Needs balance field** | Roles via StringListConverter |
| Stores | 10 fields | **Needs keycloakOrganizationId, phone** | Has upline hierarchy, needs Date→LocalDateTime migration |

**Architecture Observations**:
1. **Reseller hierarchy sudah ada**: `Stores.upline` → ManyToOne self-reference. Ini pondasi untuk multi-level reseller system.
2. **Pricing structure lengkap**: `nominal` (face value), `price` (sell price), `basePrice` (cost), `adminFee` → margin = price - basePrice
3. **Prepaid & Postpaid siap**: `denomType` (FIXED_DENOM/OPEN_AMOUNT), `requiresInquiry`, `minAmount/maxAmount`
4. **Soft delete pattern konsisten**: semua entity punya `active` + `deleted` flags
5. **Audit trail lengkap**: createdAt, updatedAt, createdBy, updatedBy di semua entity

**Gap Analysis untuk Store Onboarding (Prerequisite)**:
- ~~Missing: `keycloakOrganizationId` field di Stores entity~~ → ✅ **DONE Task 1**
- ~~Missing: `phone` field di Stores entity~~ → ✅ **DONE Task 1**
- ~~Needs fix: `Stores.createdDate/updatedDate` masih `java.util.Date`~~ → ✅ **DONE Task 1**

**Gap Analysis untuk Week 2 (After Onboarding)**:
- Missing: `Transactions` entity
- Missing: `TransactionItems` entity
- Missing: `balance` field di Users (atau di Stores?)
- Missing: `ProviderService` interface
- Missing: Transaction status enum (PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED)

---

## Session Log

### 2026-02-20 - Admin Organization Management Screen
- **Decision**: Data source = **Keycloak Organizations API** (primary) + **DB Stores** (business data: phone, email, upline)
- **Decision**: Enable/Disable org via KC org `enabled` attribute (bukan DB `stores.active`)
- **Decision**: Lihat members via KC Organization Members API (modal)
- **Decision**: Tambah reseller via **modal form** (reuse `AdminOnboardingService`), bukan redirect ke page terpisah
- **Decision**: Edit data bisnis (phone, email) → simpan ke DB `Stores`
- **Decision**: Delete = V2 (cascade ke KC org + DB terlalu complex untuk MVP)
- **NEXT**: Implementation plan → breakdown tasks

### 2026-02-20 - Store Onboarding Requirements (FINAL)
- **Decision**: Store Onboarding jadi prerequisite sebelum Week 2 (Purchase Flow)
- **Decision**: Dua jalur onboarding — self-service (Google login → form toko) + admin-created
- **Decision**: 1 Store = 1 Keycloak Organization (simplest multi-tenancy mapping)
- **Decision**: No approval process — toko langsung aktif setelah submit form
- **Decision**: Form minimal 2-3 field (Nama Toko, No. HP)
- **Decision**: Bi-directional sync antara Store (DB) dan Organization (Keycloak)
- **Decision**: Onboarding trigger → **Spring Interceptor** (bukan EventListener). Cache `hasStore` flag di session
- **Decision**: Path B password setup → **Keycloak `UPDATE_PASSWORD` required action** (email otomatis ke user)
- ✅ **CONFIRMED**: Keycloak `OrganizationsResource` API tersedia di admin-client **26.0.8**
- Identified gaps: Stores entity butuh `keycloakOrganizationId`, `phone`, dan migrasi Date→LocalDateTime
- **NEXT**: August breakdown task dari requirement ini → `Tasks.md`

### 2026-02-12 - Initial Assessment
- Julia.md created based on codebase analysis
- Week 1 (Browse Products) confirmed complete
- **Decision**: Payment/Balance system akan jadi microservice terpisah — tidak di-handle di core platform
- Brainstorm 15 feature ideas untuk backlog, dikelompokkan per tema bisnis
- PIC timeline: August (PM) — Julia draft backlog, August saring & jadwalkan
