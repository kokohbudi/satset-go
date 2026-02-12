# Julia.md - SatSetGo Product Intelligence

> **Owner**: Julia (Senior BA & Product Strategist)
> **Last Updated**: 2026-02-12
> **Sprint**: Week 2 - Purchase Prepaid

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

### Week 2: Purchase Prepaid Flow

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

### Current Sprint (Week 2-4)
- [ ] **Purchase Flow** - transaksi prepaid end-to-end (Week 2)
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
| **Balance race condition** | Saldo minus, kerugian finansial | HIGH (concurrent users) | Pessimistic locking pada deduct balance |
| **Provider downtime** | Transaksi gagal, user kecewa | MEDIUM | Auto-switching logic (multi-supplier) |
| **No unit tests** | Regression bugs saat refactor | MEDIUM | Week 4 dedicated untuk testing |
| **Stores.createdDate pakai java.util.Date** | Inkonsistensi dengan entity lain (LocalDateTime) | LOW | Migrasi ke LocalDateTime saat refactor |
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
| Stores | 10 fields | Production ready | **Has upline hierarchy** (referral model) |

**Architecture Observations**:
1. **Reseller hierarchy sudah ada**: `Stores.upline` → ManyToOne self-reference. Ini pondasi untuk multi-level reseller system.
2. **Pricing structure lengkap**: `nominal` (face value), `price` (sell price), `basePrice` (cost), `adminFee` → margin = price - basePrice
3. **Prepaid & Postpaid siap**: `denomType` (FIXED_DENOM/OPEN_AMOUNT), `requiresInquiry`, `minAmount/maxAmount`
4. **Soft delete pattern konsisten**: semua entity punya `active` + `deleted` flags
5. **Audit trail lengkap**: createdAt, updatedAt, createdBy, updatedBy di semua entity

**Gap Analysis untuk Week 2**:
- Missing: `Transactions` entity
- Missing: `TransactionItems` entity
- Missing: `balance` field di Users (atau di Stores?)
- Missing: `ProviderService` interface
- Missing: Transaction status enum (PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED)

---

## Session Log

### 2026-02-12 - Initial Assessment
- Julia.md created based on codebase analysis
- Week 1 (Browse Products) confirmed complete
- **Decision**: Payment/Balance system akan jadi microservice terpisah — tidak di-handle di core platform
- Brainstorm 15 feature ideas untuk backlog, dikelompokkan per tema bisnis
- PIC timeline: August (PM) — Julia draft backlog, August saring & jadwalkan
