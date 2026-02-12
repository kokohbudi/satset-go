# PPOB Server - Development Roadmap

**Approach**: Hybrid Feature Slicing (Option D)
**Duration**: 4 weeks
**Start Date**: 2026-02-12

---

## Foundation (Completed ✅)

- [x] Database schema (Categories, Products, ProductDenoms, ProductDenomMeta)
- [x] Repositories (CategoryRepository, ProductRepository, ProductDenomRepository, ProductDenomMetaRepository)
- [x] User management & authentication (Keycloak integration)
- [x] Entities migrated to @UuidGenerator (modern Hibernate)

---

## Week 1: Feature Slice "Browse Products" 📱

**Goal**: User bisa lihat catalog produk (categories → products → denoms)

### Day 1-2: Service Layer
- [ ] `CategoryService` - findAll, findByCode, findByType (PREPAID/POSTPAID)
- [ ] `ProductService` - findByCategory, findActiveProducts, findActiveDenoms
- [ ] `ProductDenomService` - findByProduct, findByCode, getDenomWithMeta
- [ ] Unit tests (minimal - happy path)

### Day 3-4: REST API
- [ ] `ProductController` (REST endpoints)
  - `GET /api/categories` - list all categories
  - `GET /api/categories/{code}/products` - products by category
  - `GET /api/products/{code}/denoms` - denoms by product
- [ ] Add caching with `@Cacheable` (Caffeine already configured)
- [ ] API testing (Postman/curl)

### Day 5: UI (Simple)
- [ ] Thymeleaf page: `/products` - list categories with icons
- [ ] Click category → show products grid
- [ ] Click product → show denominations with prices
- [ ] Responsive layout (already have Tailwind CSS)

**Demo Outcome**: Bisa browse product catalog dari browser

---

## Week 2: Feature Slice "Purchase Prepaid" 💳

**Goal**: User bisa beli pulsa/paket data (with mock provider)

### Day 1-2: Domain Model & Service
- [ ] Create entities:
  - `Transactions` (id, userId, totalAmount, status, createdAt, etc)
  - `TransactionItems` (transactionId, denomId, quantity, price, etc)
- [ ] `TransactionService`:
  - `createOrder(userId, denomId, quantity)` - validate & create order
  - Validate: product active, stock available (if applicable)
  - Calculate total price (price * quantity + admin_fee)
  - Set initial status: PENDING
- [ ] `BalanceService`:
  - `checkBalance(userId)` - get current balance
  - `deductBalance(userId, amount)` - with pessimistic lock (@Lock)
  - `getBalanceHistory(userId)` - transaction history
- [ ] Migration: Add `balance` column to `users` table (DECIMAL(15,2), default 0)

### Day 3: Provider Integration (Mock)
- [ ] Create `ProviderService` interface
  - `fulfillOrder(Transaction transaction)` → ProviderResponse
- [ ] `MockProviderService` implementation
  - Random success/failure (90% success rate)
  - Simulate API delay (500ms - 2s)
  - Return mock transaction reference
- [ ] Async worker (Spring @Async or @Scheduled)
  - Poll PENDING transactions
  - Call provider → update status to SUCCESS/FAILED

### Day 4: REST API
- [ ] `TransactionController`
  - `POST /api/transactions/purchase` - create new order
    - Body: `{denomCode, quantity}`
  - `GET /api/transactions/{id}` - check order status
  - `GET /api/transactions/history?page=0&size=10` - user's transaction history
- [ ] DTOs:
  - `PurchaseRequest` (denomCode, quantity)
  - `TransactionResponse` (id, status, amount, productName, createdAt)

### Day 5: UI & Testing
- [ ] Thymeleaf: Product detail page with "Buy" button
- [ ] Purchase form (select quantity, show total price, confirm button)
- [ ] Show success/error message after purchase
- [ ] Transaction history page (table with status badges)
- [ ] End-to-end test: browse → buy → check balance → verify history

**Demo Outcome**: User can purchase products, balance decreases, transaction history visible

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

### Week 1 Status: 🔲 Not Started
- [ ] Service Layer
- [ ] REST API
- [ ] UI

### Week 2 Status: 🔲 Not Started
- [ ] Transaction entities & service
- [ ] Provider mock
- [ ] Purchase API & UI

### Week 3 Status: 🔲 Not Started
- [ ] Payment service
- [ ] Payment gateway mock
- [ ] Top-up UI

### Week 4 Status: 🔲 Not Started
- [ ] Refactoring
- [ ] Admin API
- [ ] Admin UI

---

## Notes & Learnings

*(Use this section to document key decisions, gotchas, and learnings as you progress)*

### Week 1:
-

### Week 2:
-

### Week 3:
-

### Week 4:
-

---

**Last Updated**: 2026-02-12
**Next Review**: End of Week 1 (2026-02-19)
