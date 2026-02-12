# SatSetGo - Task Board

> **Owner**: August (Senior PM)
> **Last Updated**: 2026-02-12
> **Sprint**: Week 2 - Purchase Prepaid Flow

---

## 🔥 HIGH PRIORITY (DO NOW)

### Infrastructure
- [!] **BLOCKED: Commit persona files** - Ada staged files yang belum di-commit (AUGUST_PERSONA.md, JULIA_PERSONA.md, NEO_PERSONA.md, Julia.md, Tasks.md, TechSpecs.md). Harus clean dulu sebelum lanjut coding.
  - **Command**: `git add . && git commit -m "docs: Add 3-persona system (Julia BA + August PM + Neo Tech Lead)" && git push origin user-management-ui`

### Week 2: Purchase Prepaid Flow
*(Dependencies: Perlu commit persona files dulu)*

#### Day 1-2: Domain Model & Service
- [ ] **Add balance field to Users entity** - `balance DECIMAL(15,2) DEFAULT 0`, jangan lupa getter/setter
- [ ] **Create Transactions entity** - fields: id, userId (FK), totalAmount, status (enum), paymentMethod, createdAt, updatedAt
- [ ] **Create TransactionItems entity** - fields: id, transactionId (FK), denomId (FK), quantity, price, subtotal
- [ ] **Create TransactionStatus enum** - values: PENDING, PROCESSING, SUCCESS, FAILED, REFUNDED
- [ ] **Implement TransactionService** - method: createOrder(userId, denomId, quantity) dengan validasi produk aktif, hitung total
- [ ] **Implement BalanceService** - methods: checkBalance(userId), deductBalance(userId, amount) pakai pessimistic lock, getBalanceHistory(userId)

#### Day 3: Provider Integration (Mock)
- [ ] **Create ProviderService interface** - method: fulfillOrder(Transaction) → ProviderResponse
- [ ] **Implement MockProviderService** - random success/fail (90% sukses), delay 500ms-2s, return mock reference
- [ ] **Setup Async Worker** - polling PENDING transactions, call provider, update status to SUCCESS/FAILED

#### Day 4: REST API
- [ ] **Create TransactionController** - endpoints: POST /api/transactions/purchase, GET /api/transactions/{id}, GET /api/transactions/history
- [ ] **Create DTOs** - PurchaseRequest(denomCode, quantity), TransactionResponse(id, status, amount, productName, createdAt)
- [ ] **Validation** - @Valid on request body, proper error response

#### Day 5: UI & Testing
- [ ] **Product detail page** - tambah "Buy" button di catalog
- [ ] **Purchase form** - input quantity, show total price, confirm button
- [ ] **Success/Error messaging** - toast notification atau alert
- [ ] **Transaction history page** - table with status badges (PENDING=yellow, SUCCESS=green, FAILED=red)
- [ ] **End-to-end test manual** - browse → buy → check balance → verify history

---

## 📦 BACKLOG (GOOD IDEA, NOT NOW)

### Week 3: Balance Top-up (Pending Week 2)
- [-] Payment service entities (Deposits, PaymentTransactions)
- [-] MockPaymentGateway implementation
- [-] Top-up UI with payment flow

### Week 4: Admin Product Management (Pending Week 3)
- [-] AdminProductService (CRUD for categories, products, denoms)
- [-] Admin UI (dashboard, forms, tables)
- [-] Security with @PreAuthorize("hasRole('ADMIN')")

### Revenue & Pricing Features (Post-MVP)
- [-] **Reseller Tier & Dynamic Pricing** - harga berbeda per tier (Bronze/Silver/Gold/Platinum), naik otomatis dari volume transaksi
- [-] **Markup per Store** - setiap Store set markup sendiri di atas harga platform
- [-] **Komisi Upline (Rebate System)** - upline dapat komisi per transaksi downline (field `Stores.upline` sudah siap)

### Reseller Experience (Post-MVP)
- [-] **White-label Storefront** - setiap Store punya URL sendiri (subdomain atau custom domain)
- [-] **Dashboard Analytics per Store** - total transaksi, produk terlaris, profit bulanan, performa downline
- [-] **API Key untuk Reseller** - reseller besar bisa integrasi via REST API
- [-] **Bulk/Batch Transaction** - upload CSV untuk kirim pulsa massal

### Platform & Operations (Future)
- [-] **Auto-switch Supplier (Failover)** - kalau supplier H2H down, switch ke backup otomatis
- [-] **Product Price Watcher** - pantau harga dari multiple supplier, alert admin kalau ada perubahan signifikan
- [-] **Dispute & Complaint Management** - reseller submit komplain, admin investigate, eskalasi ke supplier
- [-] **Audit Log & Activity Trail** - expand createdBy/updatedBy ke action-level logging

### Product Expansion (Future)
- [-] **Postpaid Inquiry** - cek tagihan PLN/PDAM/Telkom sebelum bayar (schema sudah support)
- [-] **Produk Non-Telco** - voucher game (ML, FF, Genshin), e-money, streaming (Netflix, Spotify), token listrik
- [-] **Real Provider Integration** - Digiflazz / VIP Reseller API (swap MockProviderService)

### Growth & Engagement (Future)
- [-] **Promo & Voucher Engine** - diskon per produk, cashback volume, voucher code
- [-] **Notification Engine** - event-driven: trx sukses/gagal, saldo menipis, harga berubah
- [-] **Gamification** - badge/level reseller berdasarkan volume, leaderboard bulanan
- [-] **Referral Tracking** - dashboard performa upline-downline, commission report

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

---

## 🚨 RISKS & MITIGATION

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Balance race condition | Saldo minus, kerugian finansial | HIGH | Pessimistic locking pada deduct balance |
| Users entity belum punya balance field | Blocking Week 2 | HIGH | **Must do first** - migration sebelum coding |
| No unit tests | Regression bugs saat refactor | MEDIUM | Week 4 dedicated untuk testing |
| Stores.createdDate pakai java.util.Date | Inkonsistensi dengan entity lain | LOW | Migrasi ke LocalDateTime saat refactor |
| No pagination | Performance issue saat data besar | LOW | Tambahkan saat catalog > 100 items |

---

## 📌 SYNC CHECK (ACCOUNTABILITY)

### Pre-Week 2 Checklist:
- [ ] Week 1 fully tested manually (buka browser, browse catalog, pastikan data muncul)
- [ ] Git clean (commit semua changes sebelum mulai Week 2)
- [ ] Database seeded dengan sample data (minimal 1 category, 3 products, 5 denoms)

**Status**: ⏸️ Blocked - perlu commit persona files dulu

---

## 💬 PM NOTES

**2026-02-12**:
- Tasks.md dibuat berdasarkan ROADMAP.md Week 2 + Julia.md requirements
- Backlog features dari Julia sudah di-parse dan diprioritaskan (semua masuk BACKLOG, fokus Week 2 dulu)
- **Critical blocker**: Ada staged files yang belum commit (AUGUST_PERSONA.md, JULIA_PERSONA.md, Julia.md)
- **Recommendation**: Commit persona files dengan message "docs: Add persona system (August PM + Julia BA)"
- **Decision**: Balance field di Users, bukan di Stores (untuk Week 2 simplicity - bisa migrate ke Stores later kalau needed)

---

**Last Updated**: 2026-02-12
**Next Review**: End of Day 1 Week 2 (setelah domain model selesai)
