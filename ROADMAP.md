# PPOB Server - Development Roadmap

**Approach**: Hybrid Feature Slicing (Option D)
**Duration**: 4 weeks
**Start Date**: 2026-02-12

---

## Foundation (Completed ✅)

### Phase 0: Database Design & Setup (Completed 2026-02-12)

**Database Design Brainstorming**:
- [x] Evaluated 4 design options (Simple, Separated Tables, JSON, Hybrid)
- [x] Selected **Option D: Hybrid Approach** - balanced simplicity & structure
- [x] Schema supports both prepaid & postpaid products
- [x] Flexible metadata table for edge cases

**Entities Created**:
- [x] `Categories` - Product categories (Pulsa, Data, Game, PLN Postpaid, etc.)
- [x] `Products` - Providers per category (Telkomsel, XL, Garena, PLN)
- [x] `ProductDenoms` - Unified table for denominations (prepaid & postpaid)
- [x] `ProductDenomMeta` - Key-value metadata for specific requirements

**Enums Created**:
- [x] `CategoryType` - PREPAID, POSTPAID
- [x] `DenomType` - FIXED_DENOM, OPEN_AMOUNT

**Repositories Created**:
- [x] `CategoryRepository` - with caching & filtering
- [x] `ProductRepository` - with category filtering
- [x] `ProductDenomRepository` - with product & type filtering
- [x] `ProductDenomMetaRepository` - basic CRUD

**Code Quality**:
- [x] Migrated all entities from deprecated `@GenericGenerator` → `@UuidGenerator`
- [x] Updated existing entities (Users, Stores) to modern Hibernate pattern
- [x] Compilation verified - zero errors, zero deprecation warnings
- [x] Consistent audit fields across all entities

**Documentation**:
- [x] `ROADMAP.md` created - 4-week detailed plan
- [x] `CLAUDE.md` created - project context & coding standards

---

## Week 1: Feature Slice "Browse Products" 📱

**Goal**: User bisa lihat catalog produk (categories → products → denoms)

### Day 1-2: Service Layer ✅
- [x] `CategoryService` - findAll, findByCode, findByType (PREPAID/POSTPAID)
- [x] `ProductService` - findByCategory, findActiveProducts, findActiveDenoms
- [x] `ProductDenomService` - findByProduct, findByCode, getDenomWithMeta
- [x] DTO mapping (CategoryDTO, ProductDTO, ProductDenomDTO)
- [x] Caching with @Cacheable (standardCacheManager)

### Day 3-4: REST API ✅
- [x] `ProductCatalogController` (REST endpoints)
  - [x] `GET /api/categories` - list all categories
  - [x] `GET /api/categories/{code}` - get category by code
  - [x] `GET /api/categories/type/{type}` - filter by PREPAID/POSTPAID
  - [x] `GET /api/categories/{code}/products` - products by category
  - [x] `GET /api/products/{code}/denoms` - denoms by product
- [x] ResponseEntity with proper HTTP status codes
- [x] Caching configured and tested

### Day 5: UI (Simple) ✅
- [x] `ProductPageController` - web controller for Thymeleaf
- [x] `/products/index.html` - product catalog page
- [x] Category browsing with icons
- [x] Product grid display per category
- [x] Denomination listing with prices
- [x] Responsive layout with Tailwind CSS

**Demo Outcome**: ✅ User dapat browse product catalog lengkap dari browser

---

## Week 2: Feature Slice "Purchase Prepaid" 💳

**Goal**: User bisa beli pulsa/paket data (with mock provider)

### Day 1-2: Domain Model & Service ✅
- [x] Create entities:
  - `Transactions` (id, storeId, productDenomId, targetNumber, price, adminFee, total, status, providerRef, serialNumber)
  - `StoreMutations` (Double-Entry Ledger: amount, type, balanceAfter, referenceType, referenceId)
- [x] Enums: `TransactionStatus`, `MutationType`, `MutationReferenceType`
- [x] `BalanceService` (pessimistic lock + ledger + cache sync)
- [x] `Stores.balance` (BigDecimal cache field, default 0)
- [x] `InsufficientBalanceException` (extends BusinessException)

### Day 3: Provider Integration (Mock) ✅
- [x] `ProviderService` interface → `sendTransaction(targetNumber, denomCode, amount)`
- [x] `MockProviderService` (90% success, 500ms delay, random ref + SN)
- [x] `ProviderResponse` (Java record)

### Day 4: REST API ✅ (partial)
- [x] `TransactionController`
  - `POST /api/transactions/purchase` — purchase prepaid
  - `POST /api/transactions/topup` — admin top-up
  - `GET /api/transactions/balance/{storeId}` — check balance
- [x] DTOs: `PurchaseRequest`, `TopUpRequest` (Java records with validation)
- [ ] `GET /api/transactions/{id}` — detail transaksi
- [ ] `GET /api/transactions/history` — riwayat per store

### Day 5: UI & Testing
- [ ] Thymeleaf: Product detail page with "Buy" button
- [ ] Purchase form (select product → input number → confirm)
- [ ] Show success/error message after purchase
- [ ] Transaction history page (table with status badges)
- [ ] End-to-end test: browse → buy → check balance → verify history

**Demo Outcome**: Backend API ✅ working. UI & testing still pending.

---

## Week 3: Feature Slice "Top-up Balance" 💰

**Goal**: User bisa top-up saldo (with mock payment gateway)

### Day 1-2: Payment Service
- [ ] Create entities:
  - `Deposits` (id, userId, amount, method, status, paymentUrl, etc)
  - `PaymentTransactions` (id, depositId, gatewayRef, status, etc)
- [ ] `PaymentService`:
  - `createDeposit(userId, amount, method)` - create deposit request
  - `processPayment(depositId)` - initiate payment with gateway
  - `handleCallback(gatewayRef, status)` - process webhook
- [ ] `BalanceService` (extend):
  - `addBalance(userId, amount)` - with optimistic/pessimistic lock
  - `getBalanceHistory(userId)` - include deposits & purchases

### Day 2-3: Payment Gateway Mock
- [ ] Create `PaymentGatewayService` interface
  - `createPaymentUrl(depositId, amount)` → PaymentUrl
  - `verifyPayment(gatewayRef)` → PaymentStatus
- [ ] `MockPaymentGateway` implementation
  - Generate fake payment URL (e.g., /mock-payment/{id})
  - Auto-approve after 5 seconds (simulated webhook callback)
- [ ] Webhook endpoint: `POST /api/payment/webhook`
  - Verify signature (mock)
  - Update deposit status
  - Add balance if success

### Day 4: REST API
- [ ] `BalanceController`
  - `POST /api/balance/topup` - create deposit & return payment URL
    - Body: `{amount, method}`
  - `GET /api/balance/current` - get current balance
  - `GET /api/balance/history?type=ALL|DEPOSIT|PURCHASE` - balance movements
- [ ] DTOs:
  - `TopupRequest` (amount, method)
  - `DepositResponse` (id, paymentUrl, status, amount)

### Day 5: UI
- [ ] Thymeleaf: Top-up page
  - Amount input (with preset buttons: 50k, 100k, 200k, 500k)
  - Payment method selection (Mock Gateway)
  - Submit → redirect to payment URL
- [ ] Show current balance in navbar (always visible)
- [ ] Balance history page (table: date, type, amount, balance after)
- [ ] Test complete flow: top-up → balance increases → purchase → balance decreases

**Demo Outcome**: Full working flow: top-up balance → buy products → balance updates correctly

---

## Week 4: Refactor + Feature Slice "Admin Product Management" ⚙️

**Goal**: Admin can manage products via UI (CRUD operations)

### Day 1: Code Refactoring
- [ ] Review all services - identify duplicated code
- [ ] Extract `BaseService<T>` (optional) - common CRUD patterns
  - save, findById, findAll, delete, etc.
- [ ] Cleanup code duplication in controllers
- [ ] Review tests - ensure coverage for critical paths
- [ ] Update documentation (inline comments where needed)

### Day 2-3: Admin Service & API
- [ ] `AdminProductService` extends ProductService
  - `createCategory(CategoryDTO)` - validate & create
  - `updateCategory(id, CategoryDTO)` - validate & update
  - `deleteCategory(id)` - soft delete (set deleted=true)
  - Same methods for Product and ProductDenom
- [ ] `AdminController` (REST API)
  - `POST /api/admin/categories` - create category
  - `PUT /api/admin/categories/{id}` - update category
  - `DELETE /api/admin/categories/{id}` - soft delete
  - Same endpoints for `/admin/products` and `/admin/denoms`
- [ ] Security:
  - Add `@PreAuthorize("hasRole('ADMIN')")` on all admin endpoints
  - Test with ADMIN and non-ADMIN users

### Day 4-5: Admin UI
- [ ] Thymeleaf: Admin dashboard layout
  - Sidebar navigation (Categories, Products, Denoms, Users)
  - Main content area
- [ ] Categories management:
  - List table (with edit/delete buttons)
  - Create/Edit form (modal or separate page)
  - Toggle active status
- [ ] Products management:
  - List table (filterable by category)
  - Create/Edit form (select category, upload icon)
  - Toggle active status
- [ ] Denoms management:
  - List table (filterable by product)
  - Create/Edit form (all fields including pricing)
  - Toggle active status
- [ ] **BONUS** (if time permits):
  - Bulk upload via CSV
  - Image upload for product icons
- [ ] Test: Create new product → verify it appears in user catalog

**Demo Outcome**: Admin can fully manage product catalog without touching database

---

## Checkpoint After Week 4 🎯

### What You'll Have Built:
- ✅ Complete product catalog browsing (categories → products → denoms)
- ✅ Working purchase flow (order creation, balance deduction, transaction tracking)
- ✅ Balance top-up system (deposits, payment gateway integration)
- ✅ Admin product management (full CRUD via UI)
- ✅ Clean architecture (services, repositories, controllers separated)
- ✅ Mock integrations (easy to replace with real providers later)

### Evaluate & Plan Next Phase:

#### Option A: Real Provider Integration (2 weeks)
- Integrate real PPOB provider (Digiflazz, VIP Reseller, etc.)
- Replace `MockProviderService` with real API calls
- Handle API errors, retries, timeouts
- Implement reconciliation (match our transactions with provider's)
- Callback/webhook handling from provider

#### Option B: Postpaid Products (1-2 weeks)
- Inquiry API (check bills for PLN, PDAM, Telkom, etc.)
- Different purchase flow (inquiry first → then payment)
- Handle customer ID validation
- UI for postpaid inquiry & payment

#### Option C: Advanced Features (2-3 weeks)
- Dynamic pricing engine (margin per user level/tier)
- Promo codes & discount system
- Reporting dashboard (sales analytics, profit tracking)
- Notifications (email/SMS after purchase, low balance alerts)
- Referral system (existing upline field in Stores)

#### Option D: Production Ready (1-2 weeks)
- Real payment gateway integration (Midtrans, Xendit, etc.)
- Production deployment (VPS, Docker, AWS, etc.)
- Monitoring & logging (structured logs, error tracking)
- Performance optimization (query tuning, caching strategy)
- Security audit (SQL injection, XSS, CSRF protection)
- Load testing

---

## Daily Workflow Template 📝

### Morning Routine:
1. ☕ Review today's task list
2. 🎯 Set ONE clear goal (e.g., "Finish ProductService implementation")
3. 📖 Quick review of yesterday's code

### During Work:
4. 💻 Code → Test → Commit (small, frequent commits)
5. ⏱️ Stuck >30 minutes? Ask for help or skip to next task
6. 📝 Update TODO as you discover new tasks

### Evening Routine:
7. ✅ Mark completed tasks
8. 🧪 Quick manual testing (run app, click around)
9. 💾 Git commit & push
10. 📋 Prepare tomorrow's task list

### Weekly Routine:
- **Friday Evening**:
  - Review week's progress
  - Demo all features to yourself
  - Celebrate wins (even small ones!)
- **Sunday Evening**:
  - Plan next week's tasks
  - Prioritize based on learnings

---

## Anti-Burnout Guidelines ✅

### DO:
- ✅ **Set realistic goals** - 1 feature per week is enough
- ✅ **Ship incomplete** - "working but ugly" > "perfect but unfinished"
- ✅ **Celebrate progress** - every commit is progress
- ✅ **Ask for help** - when stuck >30 min
- ✅ **Take breaks** - Pomodoro technique (25 min work, 5 min break)
- ✅ **Rest on weekends** - no coding 24/7

### DON'T:
- ❌ **Add scope mid-week** - stick to the plan
- ❌ **Perfectionism** - "good enough" is good enough
- ❌ **Compare to others** - your pace is your pace
- ❌ **Skip testing** - bugs compound quickly
- ❌ **Work when tired** - rest is productive

---

## Progress Tracking

Update this section weekly:

### Week 1 Status: ✅ Complete (2026-02-12)
**All tasks finished - Service, API, and UI working**
- [x] Service Layer (CategoryService, ProductService, ProductDenomService)
- [x] REST API (ProductCatalogController with caching)
- [x] UI (Product catalog browsing with Thymeleaf)

### Week 2 Status: 🟢 Backend Done (2026-02-24)
**Purchase flow backend fully implemented with Double-Entry Ledger**
- [x] Transaction entities & StoreMutations ledger
- [x] Provider mock (90% success)
- [x] Purchase API (3 endpoints)
- [ ] Purchase UI (Thymeleaf)
- [ ] Missing endpoints: GET /{id}, GET /history

### Week 3 Status: ⏸️ Blocked (Waiting for Week 2)
- [ ] Payment service
- [ ] Payment gateway mock
- [ ] Top-up UI

### Week 4 Status: ⏸️ Blocked (Waiting for Week 3)
- [ ] Refactoring
- [ ] Admin API
- [ ] Admin UI

---

## Notes & Learnings

*(Use this section to document key decisions, gotchas, and learnings as you progress)*

### Phase 0 (Foundation - 2026-02-12):

**Key Decisions**:
1. **Database Design**: Selected Hybrid Approach (Option D)
   - Rationale: Balance between simplicity and structure
   - Single table for denoms with nullable columns for prepaid/postpaid specific fields
   - Separate metadata table for flexibility without bloating main table

2. **UUID Generator Migration**:
   - Migrated from deprecated `@GenericGenerator` to `@UuidGenerator`
   - Applied to all entities (new + existing Users/Stores)
   - No compilation warnings, cleaner code

3. **Development Approach**: Hybrid Feature Slicing
   - Vertical slices per feature (Week 1-4)
   - Extract shared patterns only after they emerge 2-3 times
   - Mock-first for external integrations

**Technical Gotchas**:
- Hibernate 7.x (Spring Boot 4.0.1) deprecates `@GenericGenerator` since 6.5
- Java 25 + Lombok shows `Unsafe.objectFieldOffset` warnings (safe to ignore)
- PostgreSQL requires `columnDefinition = "uuid"` for UUID columns

**Next Steps**:
- Week 1: Implement service layer for product browsing
- Consider seeding initial data (categories, products) for testing

### Week 1 (2026-02-12):

**Completed Features**:
- ✅ Service layer with DTO mapping pattern
- ✅ REST API with proper HTTP status codes
- ✅ Thymeleaf UI for product catalog browsing
- ✅ Caching implemented (standardCacheManager)

**Key Learnings**:
1. **Service Layer Pattern**:
   - Constructor injection over @Autowired (cleaner, testable)
   - `@Transactional(readOnly = true)` as default on service class
   - DTO mapping in service layer (not in controller)
   - Separate DTO classes per entity (CategoryDTO, ProductDTO, ProductDenomDTO)

2. **Caching Strategy**:
   - Used `standardCacheManager` for categories (frequently accessed, rarely changed)
   - Cache key strategy: `#param` for method parameters
   - Cache eviction strategy not yet implemented (will add in admin CRUD later)

3. **API Design**:
   - RESTful resource naming: `/api/categories`, `/api/products`
   - Proper use of HTTP status: 200 OK, 404 Not Found
   - `ResponseEntity<T>` for explicit status control
   - PathVariable for resource identification

4. **UI Patterns**:
   - Separate controller for web pages (`ProductPageController`)
   - Model attributes for passing data to Thymeleaf
   - Tailwind CSS for responsive design (already configured)

**Technical Notes**:
- No unit tests yet (deferred to Week 4 refactoring phase)
- Product seeding needed for testing (currently empty database)
- Consider adding pagination for large product catalogs (future enhancement)

**Next Week Preview**:
- Week 2 will introduce transactions, which requires balance field in Users table
- Mock provider pattern will be established (interface → mock → real implementation later)

### Week 2 (2026-02-24):

**Key Decisions**:
1. **Double-Entry Ledger** (buku tabungan) menggantikan desain awal
   - `StoreMutations` = source of truth (immutable)
   - `Stores.balance` = read cache (sync saat mutasi)
   - Alasan: Auditability 100%, zero desync risk, extensible
2. **Polymorphic Reference**: `referenceType + referenceId` instead of nullable FK
   - Extensible: TOP_UP, PURCHASE, REFUND, ADJUSTMENT
3. **Saga Pattern**: Purchase flow not wrapped in single @Transactional
   - Each step (deduct, provider, refund) commits independently

**Architecture**:
- Pessimistic lock pada `Stores` row sebagai serialization point
- `StoreMutations` immutable (only `createdAt`, no `updatedAt`)
- Balance = `Stores.balance` cache, reconcilable dari ledger terakhir

**Files Created**: 10 new + 2 modified (see walkthrough)

### Week 3:
-

### Week 4:
-

---

**Last Updated**: 2026-02-24 (Week 2 Backend Complete ✅)
**Next Review**: After Purchase UI complete
**Current Phase**: Week 2 Backend ✅ → Purchase UI & Testing next 🚀
