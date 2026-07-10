# SatSetGo - Technical Specifications

> **Owner**: Neo (Chief Technical Architect)
> **Last Updated**: 2026-02-25 (Session 5)
> **Current Sprint**: Port Boundary & Config Consolidation COMPLETE

---

## 📐 Architecture Overview

**Stack**:
- Framework: Spring Boot 4.0.1
- Java: 25 (Virtual Threads enabled)
- Database: PostgreSQL 16+
- ORM: Hibernate 7.x (JPA)
- Auth: Keycloak OAuth2/OIDC
- Cache: Caffeine (2-tier: fast + slow)
- UI: Thymeleaf + Tailwind CSS

**Deployment**:
- Target: Linux server (Ubuntu/Debian)
- Process: SystemD service
- Reverse Proxy: Nginx (recommended)

---

## 🗄️ Database Design

### Schema Philosophy
- **UUID Primary Keys**: Distributed-friendly, non-sequential (security)
- **Soft Delete**: `deleted BOOLEAN DEFAULT FALSE` (preserve audit trail)
- **Optimistic Locking**: `@Version` on entities with concurrent updates
- **Audit Fields**: `createdAt`, `updatedAt`, `createdBy`, `updatedBy` (mandatory)

### Indexing Strategy
```sql
-- High-traffic queries (add as needed)
CREATE INDEX idx_products_category_active ON products(category_id, active) WHERE deleted = FALSE;
CREATE INDEX idx_denoms_product_type ON product_denoms(product_id, denom_type) WHERE active = TRUE;
CREATE INDEX idx_transactions_user_status ON transactions(user_id, status, created_at DESC);
```

### Current Schema
See `CLAUDE.md` for entity details. Schema managed by Hibernate DDL (`update` mode).

---

## 🔐 Security Specifications

### Authentication & Authorization
- **Keycloak Integration**: OAuth2 Resource Server
- **Roles**: `USER`, `ADMIN`, `RESELLER` (managed in Keycloak)
- **Endpoint Protection**:
  ```java
  @PreAuthorize("hasRole('ADMIN')") // Admin-only
  @PreAuthorize("isAuthenticated()") // Logged-in users
  ```

### Input Validation
- **DTOs**: Use `@Valid` + JSR-303 annotations (`@NotNull`, `@Size`, `@Pattern`)
- **SQL Injection**: Prevented by JPA (parameterized queries)
- **XSS**: Thymeleaf auto-escapes by default
- **CSRF**: Enabled for forms (Spring Security default)

### Sensitive Data
- **Balance Operations**: Use pessimistic locking (`@Lock(PESSIMISTIC_WRITE)`)
- **Transaction Logs**: Never delete (audit trail)
- **API Keys** (future): Hash with bcrypt, never log in plaintext

---

## ⚡ Performance Specifications

### Caching Strategy
```java
// Fast cache: 10k entries, 5 min TTL (hot data)
@Cacheable(value = "categories", cacheManager = "fastCacheManager")

// Slow cache: 5k entries, 60 min TTL (stable data)
@Cacheable(value = "products", cacheManager = "slowCacheManager")
```

**Eviction**:
- `@CacheEvict` on create/update/delete operations
- Manual flush on admin bulk changes

### Query Optimization
- **N+1 Problem**: Use `@EntityGraph` or JOIN FETCH
- **Pagination**: Implement when dataset > 100 items
- **Batch Size**: Configured at 25 (Hibernate property)

### Virtual Threads (Java 25)
- **Enabled**: `spring.threads.virtual.enabled=true`
- **Benefit**: High concurrency for I/O-bound operations (DB, HTTP calls)
- **Caution**: Don't use `synchronized` blocks (pinning issue)
- **Config Cleanup (M-6)**: Removed `tomcat.max-threads` & `min-spare-threads` (irrelevant with virtual threads)

---

## 🏗️ Design Patterns

### Layered Architecture
```
Controller (REST/Web)
    ↓
Service (Business Logic + @Transactional)
    ↓
Repository (JPA Queries)
    ↓
Entity (Domain Model)
```

### DTO Pattern
- **Never expose entities directly** in REST responses
- **Mapping**: Manual (constructor) or MapStruct (future)
- **Naming**: `EntityDTO`, `EntityRequest`, `EntityResponse`

### Service Pattern
```java
@Service
@Transactional(readOnly = true) // Default
public class EntityService {

    @Transactional // Override for writes
    public Entity create(EntityDTO dto) {
        // Validate → Map → Save
    }
}
```

### Repository Pattern
```java
@Repository
public interface EntityRepository extends JpaRepository<Entity, UUID> {
    // Naming: findBy<Field><Condition>
    Optional<Entity> findByCodeAndActiveTrueAndDeletedFalse(String code);

    // Custom queries (if needed)
    @Query("SELECT e FROM Entity e WHERE ...")
    List<Entity> customQuery();
}
```

---

## 🧪 Testing Strategy

### Unit Tests
- **Coverage Target**: 70%+ for services
- **Framework**: JUnit 5 + Mockito
- **Pattern**: AAA (Arrange-Act-Assert)

### Integration Tests
- **Database**: Testcontainers (PostgreSQL)
- **Auth**: Mock Keycloak (or test realm)

### Manual Testing (MVP Phase)
- End-to-end flows via browser
- Document test scenarios in `TESTING.md` (future)

---

## 📡 API Design

### REST Conventions
```
GET    /api/resource       - List all
GET    /api/resource/{id}  - Get by ID
POST   /api/resource       - Create
PUT    /api/resource/{id}  - Update
DELETE /api/resource/{id}  - Delete (soft)
```

### Response Format
```json
{
  "id": "uuid",
  "field": "value",
  "createdAt": "2026-02-12T10:30:00",
  "updatedAt": "2026-02-12T10:30:00"
}
```

### Error Response
```json
{
  "timestamp": "2026-02-12T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/resource"
}
```

---

## 🔄 Concurrency Control

### Optimistic Locking
```java
@Version
private Long version;
```
- **Use case**: General entity updates (low conflict)
- **Behavior**: Throws `OptimisticLockException` if version mismatch

### Pessimistic Locking
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Users> findById(UUID id);
```
- **Use case**: Balance deduction (high conflict risk)
- **Behavior**: Row-level lock (other transactions wait)

---

## 🚀 Deployment Checklist

### Pre-Production
- [ ] Change DDL mode: `hibernate.hbm2ddl.auto=validate`
- [ ] Enable production logging (ERROR level)
- [ ] Configure connection pool (HikariCP tuning)
- [ ] Setup monitoring (Prometheus + Grafana future)
- [ ] Backup strategy (PostgreSQL daily dumps)

### Environment Variables
```bash
# Application secrets (all moved to .env, 2026-02-25)
KEYCLOAK_REALM=satset-go
KEYCLOAK_BASE_URL=http://localhost:9999
KEYCLOAK_CLIENT_ID=satsetgo-client
KEYCLOAK_CLIENT_SECRET=<secret>
DB_URL=jdbc:postgresql://localhost:5432/satset_go
DB_USERNAME=admin
DB_PASSWORD=password

# Production example
SPRING_PROFILES_ACTIVE=prod
KEYCLOAK_REALM=satsetgo-prod
KEYCLOAK_BASE_URL=https://auth.satsetgo.com
DB_URL=jdbc:postgresql://prod-host:5432/satsetgo_prod
DB_USERNAME=satsetgo_user
DB_PASSWORD=<strong-password>
```

---

## 📋 Technical Debt Register

> **Last Audit**: 2026-02-25 by Neo

### ✅ Resolved Debt

- ~~`Stores.createdDate` uses `java.util.Date`~~ → ✅ **DONE — Task 1** (migrated to `LocalDateTime`)
- ~~`Stores` missing `keycloakOrganizationId`, `phone`~~ → ✅ **DONE — Task 1**
- ~~`StoreRepository` generic `Long` instead of `UUID`~~ → ✅ **DONE — Task 1** (latent bug fixed)
- ~~`createPurchase()` missing `@Transactional`~~ → ✅ **FIXED 2026-02-25** (financial atomicity)
- ~~`AdminResellerController` no `@PreAuthorize`~~ → ✅ **FIXED 2026-02-25** (privilege escalation closed)
- ~~`TransactionController` no `@PreAuthorize`~~ → ✅ **FIXED 2026-02-25** (method-level security added)
- ~~`IdentityDomainService` mutable cached set mutation~~ → ✅ **FIXED 2026-02-25** (defensive copy)
- ~~`UserSessionControllerAdvice` injects concrete `KeycloakAdminClientService`~~ → ✅ **FIXED 2026-02-25** (H-2: inject `KeycloakIdentityPort` + `getMenuRoles` added to port)
- ~~`RegistrationDomainService` injects `UserDomainService` (cross-context)~~ → ✅ **FIXED 2026-02-25** (H-4: replaced with `OnboardingUserPort`)
- ~~`TransactionController` leaks domain entities to HTTP layer~~ → ✅ **FIXED 2026-02-25** (H-5: introduced `TransactionSummary` domain record, use cases return summary not entity)

### 🔴 Critical — Architectural

| ID | Issue | File(s) | Impact |
|---|---|---|---|
| **C-1** | Domain models are JPA `@Entity` objects — violates hexagonal boundary | All `domain/model/*.java` | Domain layer coupled to persistence infrastructure |
| **C-2** | Cross-context JPA FK coupling: `Transactions` → `Stores`, `Users` → `Stores`, `Transactions` → `ProductDenoms` | `Transactions.java`, `Users.java`, `StoreMutations.java` | Bounded contexts cannot evolve independently |

### ✅ High — Architecture Violations (RESOLVED)

| ID | Issue | Fix | Resolved |
|---|---|---|---|
| **H-1** | `shared` package imports JPA adapter repos directly | Replaced with port interfaces (`CategoryRepositoryPort`, `ProductRepositoryPort`, etc.) | ✅ 2026-02-25 |
| **H-3** | Port interfaces reference Keycloak SDK (`UserRepresentation`) | Created domain record `GroupMemberInfo`; adapter boundary converts `UserRepresentation` → `GroupMemberInfo` | ✅ 2026-02-25 |
| **H-6** | `UserDTO` holds `Stores` JPA entity reference | Replaced with `UUID storeId`; updated in 5 consumers | ✅ 2026-02-25 |

### ✅ Medium — Correctness (RESOLVED)

| ID | Issue | Fix | Resolved |
|---|---|---|---|
| **M-1** | `Transactions` missing `@Version` | Added `@Version private Long version;` | ✅ 2026-02-25 |
| **M-4** | `RegistrationHelper` uses `Random` (not `SecureRandom`) | Replaced with `SecureRandom` | ✅ 2026-02-25 |
| **M-8** | Cache name collision (`findAll()` + `findByType()` → `"categories"`) | Split into `"categoriesAll"` + `"categoriesByType"` | ✅ 2026-02-25 |

### ✅ Medium — Configuration (RESOLVED)

| ID | Issue | Fix | Resolved |
|---|---|---|---|
| **M-3** | Mixed pessimistic + optimistic locking on `Stores.balance` | Documented both strategies (financial ops use pessimistic, general use optimistic) | ✅ 2026-02-25 |
| **M-5** | Role prefix magic strings (works by coincidence) | Centralized in `OmniConstants`: `ROLE_PREFIX_REALM`, `ROLE_PREFIX_CLIENT`, 7 permission constants; updated 4 controllers | ✅ 2026-02-25 |
| **M-6** | Tomcat `max-threads: 200` (irrelevant with virtual threads) | Removed; kept only `max-connections` | ✅ 2026-02-25 |
| **M-10** | Hardcoded `keycloak.realm: satset-go` | Externalized to `.env` (KEYCLOAK_REALM=satset-go); application.yml references `${KEYCLOAK_REALM}` | ✅ 2026-02-25 |

### 🟡 Medium — Remaining Fragility

| ID | Issue | File(s) |
|---|---|---|
| **M-2** | `StoreMutations` missing `@Version` (append-only, but inconsistent with pattern) | `StoreMutations.java` |
| **M-7** | `ProductDenoms.metadata` `@Transient` field — null in all paths except `getDenomWithMeta()` | `ProductDenoms.java:93-94` |
| **M-9** | `StoreMutationJpaRepository.findTopBy...` accepts `Stores` entity instead of `UUID` | `StoreMutationJpaRepository.java:15` |

### 🟢 Low — Hygiene & Consistency

| ID | Issue | File(s) |
|---|---|---|
| **L-1** | Test coverage severely limited — only 4 test files, 0 for catalog/onboarding/controllers | `src/test/java/**` |
| **L-2** | Tests mock concrete adapters (`UserJpaRepository`) instead of port interfaces | `UserDomainServiceTest.java` |
| **L-3** | Duplicated password-change logic (`UserDomainService` deprecated but not removed) | `UserDomainService.java`, `IdentityDomainService.java` |
| **L-4** | `Stores` uses `createdDate`/`updatedDate` while all other entities use `createdAt`/`updatedAt` | `Stores.java:43-47` |
| **L-5** | Dead commented-out code in `KeycloakLoginEventListener` | `KeycloakLoginEventListener.java:31,41` |
| **L-6** | `DataSeeder` not idempotent for partial runs | `DataSeeder.java:47-49` |
| **L-7** | Inconsistent exception types: `RuntimeException` vs `ResourceNotFoundException` for store-not-found | `BalanceDomainService.java` |
| **L-8** | No pagination on product listing (acceptable until 100+ products) | — |
| **L-9** | `KeycloakLoginEventListener` still auto-creates Store → **Remove in Task 5** | `KeycloakLoginEventListener.java` |

### 📊 Monitoring

- Review debt register every sprint
- Address when it blocks new features or causes bugs
- **Priority fix order**: C-1/C-2 (structural) → H-3/H-5/H-6 (introduce domain DTOs) → M-1/M-4 (correctness)

---

---

## 🏛️ Store Onboarding — Technical Blueprint

### 1. Schema Changes: `Stores` Entity ✅ DONE (Task 1)

```java
// IMPLEMENTED in Stores.java
@Column(name = "keycloak_organization_id")
private String keycloakOrganizationId;  // UUID string dari Keycloak

private String phone;                   // Nomor HP owner toko

@CreatedDate
private LocalDateTime createdDate;      // ✅ LocalDateTime (was java.util.Date)

@LastModifiedDate
private LocalDateTime updatedDate;      // ✅ LocalDateTime (was java.util.Date)
```

> ✅ **DB Migration**: `ddl-auto: update` aktif — Hibernate auto-ALTER TABLE saat app restart. Kolom `phone` dan `keycloak_organization_id` otomatis ditambahkan. `StoreRepository` generic juga difix: `Long` → `UUID`.

---

### 2. Keycloak Organization API — New Methods

```java
// KeycloakAdminClientService.java — 3 method baru

/** Create Keycloak Organization. Returns orgId. */
public String createOrganization(String orgName) {
    OrganizationRepresentation org = new OrganizationRepresentation();
    org.setName(orgName);
    org.setEnabled(true);

    try (Response response = keycloak.realm(realm).organizations().create(org)) {
        if (response.getStatus() != 201) {
            throw new BusinessException("Failed to create Keycloak organization: " + response.getStatus());
        }
        // Extract ID from Location header: .../organizations/{id}
        String location = response.getHeaderString("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}

/** Add user as member of an Organization. */
public void addMemberToOrganization(String orgId, String userId) {
    try (Response response = keycloak.realm(realm)
            .organizations().get(orgId)
            .members().addMember(userId)) {
        if (response.getStatus() != 201 && response.getStatus() != 204) {
            throw new BusinessException("Failed to add member to org: " + response.getStatus());
        }
    }
}

/** Create reseller user with UPDATE_PASSWORD required action. */
public String createResellerUser(String username, String fullname, String email) throws BusinessException {
    UserRepresentation userRep = keycloakAdminClientBusiness
            .prepareUserRepresentation(username, fullname, email);
    userRep.setRequiredActions(List.of("UPDATE_PASSWORD")); // ← email set-password
    userRep.setEmailVerified(false);

    Response resp = keycloak.realm(realm).users().create(userRep);
    return keycloakAdminClientBusiness.extractCreatedUserId(resp);
}
```

---

### 3. StoreOnboardingInterceptor — Design

```java
@Component
public class StoreOnboardingInterceptor implements HandlerInterceptor {

    private static final String SESSION_HAS_STORE = "hasStore";
    private final StoreRepository storeRepository; // atau UserRepository

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res,
                             Object handler) throws Exception {

        // Skip non-MVC handlers (static resources, actuator, etc.)
        if (!(handler instanceof HandlerMethod)) return true;

        // Cek apakah user sudah authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return true; // belum login, biarkan Spring Security handle
        }

        // Ambil dari session cache dulu
        HttpSession session = req.getSession(false);
        if (session != null) {
            Boolean hasStore = (Boolean) session.getAttribute(SESSION_HAS_STORE);
            if (Boolean.TRUE.equals(hasStore)) return true;
            if (Boolean.FALSE.equals(hasStore)) {
                res.sendRedirect("/onboarding");
                return false;
            }
        }

        // Cache miss — query DB
        UserDTO userDTO = (session != null)
            ? (UserDTO) session.getAttribute(OmniConstants.SESSION_USER_DTO)
            : null;

        if (userDTO == null) return true; // session kosong, skip

        boolean hasStore = storeRepository.existsByUserIdAndDeletedFalse(userDTO.getId());
        req.getSession(true).setAttribute(SESSION_HAS_STORE, hasStore);

        if (!hasStore) {
            res.sendRedirect("/onboarding");
            return false;
        }

        return true;
    }
}
```

**Path Exclusions (wajib di WebMvcConfigurer)**:
```java
@Override
public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(storeOnboardingInterceptor)
            .excludePathPatterns(
                "/onboarding", "/onboarding/**",
                "/login", "/logout", "/error",
                "/api/**",
                "/css/**", "/js/**", "/images/**", "/webjars/**",
                "/actuator/**"
            );
}
```

---

### 4. Rollback Strategy — Distributed Operation

> ⚠️ **Critical edge case** yang August flagging: Keycloak org berhasil tapi DB gagal.

Kita punya 2 operasi di sistem berbeda — **tidak ada 2-phase commit**. Strategi yang pragmatis:

```
Flow: createOrganization() → addMember() → saveStore()
```

| Step | Gagal? | Rollback Action |
|---|---|---|
| `createOrganization()` | Ya | Tidak ada efek — DB belum disentuh |
| `addMemberToOrganization()` | Ya | Panggil `deleteOrganization(orgId)` di catch block |
| `saveStore()` (DB) | Ya | `@Transactional` rollback DB; panggil `deleteOrganization(orgId)` di catch |

```java
@Transactional
public void onboardStore(String userId, String orgName, String phone) throws BusinessException {
    String orgId = null;
    try {
        // Step 1: Keycloak (luar @Transactional — tidak bisa rollback otomatis)
        orgId = keycloakService.createOrganization(orgName);
        keycloakService.addMemberToOrganization(orgId, userId);

        // Step 2: DB (dalam @Transactional — auto rollback jika exception)
        Stores store = new Stores();
        store.setName(orgName);
        store.setPhone(phone);
        store.setKeycloakOrganizationId(orgId);
        store.setActive(true);
        storeRepository.save(store);

        // Step 3: Link User → Store
        Users user = userRepository.findByProviderUserId(userId).orElseThrow();
        user.setStores(store);
        userRepository.save(user);

        // Step 4: Update session cache
        updateSessionHasStore(true);

    } catch (Exception e) {
        // Compensating transaction: hapus org di Keycloak jika sudah terbuat
        if (orgId != null) {
            try { keycloakService.deleteOrganization(orgId); }
            catch (Exception ex) { log.error("Keycloak org cleanup failed: {}", orgId, ex); }
            // → Log untuk manual cleanup jika deleteOrganization gagal
        }
        throw new BusinessException("Store onboarding failed: " + e.getMessage(), e);
    }
}
```

> **Neo's Note**: Ini adalah **Saga Pattern (Compensating Transactions)** — solusi standar untuk microservice tanpa distributed transaction manager. Tidak perlu Kafka/XA untuk skala ini.

---

### 5. API Contract

#### Path A — Self-service
```
GET  /onboarding          → Tampilkan form
POST /onboarding          → Submit onboarding
  Body (form): storeName (required, 3-50 char), phone (optional, pattern: 08xx)
  Success: redirect /dashboard
  Error:   redirect /onboarding?error=true
```

#### Path B — Admin-created
```
POST /admin/resellers     → Buat reseller baru (admin only)
  Body (JSON): username, email, fullname, storeName, phone, role, uplineId (optional)
  Response 201: { "userId": "...", "storeId": "...", "orgId": "..." }
  Response 400: validation error
  Response 500: onboarding failed (dengan rollback)
```

---

### 6. Edge Cases & Threat Model

| Case | Problem | Mitigasi |
|---|---|---|
| User submit form 2x (double-click) | 2 Stores + 2 Orgs dibuat | Cek `store != null` sebelum create. Unique constraint di DB pada `user_id`. |
| Org name collision di Keycloak | Keycloak tolak jika nama duplikat | Nama org pakai `storeName + "_" + userId.substring(0,8)` sebagai suffix |
| Interceptor hit tapi `UserDTO` null di session | NPE / infinite redirect | Guard: jika `userDTO == null`, skip interceptor (Spring Security handle) |
| Admin create user tapi email tidak valid | Keycloak create user tapi email bounce | Validasi format email di layer DTO sebelum kirim ke Keycloak |
| `deleteOrganization` gagal saat rollback | Orphan org di Keycloak | Log `ERROR` dengan `orgId` untuk manual cleanup. Alert admin. |

---

**Last Updated**: 2026-02-25
**Task 1 Status**: ✅ DONE — `Stores.java` updated, `StoreRepository.java` fixed
**Security Hotfix**: ✅ DONE 2026-02-25 — `@Transactional` purchase flow, `@PreAuthorize` admin/transaction endpoints, cached set defensive copy
**Next Review**: Setelah Task 3 (Interceptor) selesai — Neo review implementasi sebelum test

---

---

## 🗂️ Admin Product Management — Technical Blueprint

> **Neo Design**: 2026-03-02
> **Status**: READY FOR IMPLEMENTATION
> **Scope**: CRUD penuh untuk Categories, Products, ProductDenoms via Admin UI + REST API

---

### 0. Analisis Existing Codebase

**Yang sudah ada:**
- Entities: `Categories`, `Products`, `ProductDenoms`, `ProductDenomMeta` — siap dipakai
- Port out: `CategoryRepositoryPort`, `ProductRepositoryPort`, `DenomRepositoryPort` — punya `save()`, tapi **belum ada** `findById()` untuk Category/Product, dan belum ada method "admin variant" (tanpa `active=true AND deleted=false` filter)
- Domain services: `CategoryDomainService`, `ProductDomainService`, `DenomDomainService` — read-only, belum ada write logic
- Browse use cases: `BrowseCategoriesUseCase`, `BrowseProductsUseCase`, `BrowseDenomsUseCase` — buat reseller, bukan admin

**Yang perlu dibuat:**
- Port in (use cases): Manage variants
- Request DTOs
- Domain service extensions (write operations + cache eviction)
- REST API controller
- Thymeleaf page controller + templates

---

### 1. Port Out Extensions (Minimal Additions)

Tambah method ke existing port interfaces — **hanya yang dibutuhkan**:

```java
// CategoryRepositoryPort.java — tambah:
Optional<Categories> findById(UUID id);
List<Categories> findAllByOrderBySortOrder(); // admin: semua, tanpa filter active/deleted
boolean existsByCodeAndIdNot(String code, UUID id); // uniqueness check saat update

// ProductRepositoryPort.java — tambah:
Optional<Products> findById(UUID id);
List<Products> findByCategoryIdOrderBySortOrder(UUID categoryId); // admin: semua
boolean existsByCodeAndIdNot(String code, UUID id);

// DenomRepositoryPort.java — tambah:
List<ProductDenoms> findByProductIdOrderBySortOrder(UUID productId); // admin: semua
boolean existsByCodeAndIdNot(String code, UUID id);
void deleteMetaByDenomId(UUID denomId); // DenomMetaRepositoryPort, bukan di sini
```

> `findById()` untuk `Categories` dan `Products` belum ada di port — JpaRepository punya, tapi domain service tidak bisa akses karena inject port bukan JpaRepository.

---

### 2. New Port In — Use Case Interfaces

**File baru** di `catalog/domain/port/in/`:

```java
// ManageCategoriesUseCase.java
public interface ManageCategoriesUseCase {
    List<Categories> findAllForAdmin();
    Optional<Categories> findById(UUID id);
    Categories create(CreateCategoryRequest req);
    Categories update(UUID id, UpdateCategoryRequest req);
    void softDelete(UUID id);
}

// ManageProductsUseCase.java
public interface ManageProductsUseCase {
    List<Products> findAllForAdmin();
    List<Products> findByCategoryForAdmin(UUID categoryId);
    Optional<Products> findById(UUID id);
    Products create(CreateProductRequest req);
    Products update(UUID id, UpdateProductRequest req);
    void softDelete(UUID id); // cascade: soft-delete semua denoms juga
}

// ManageDenomsUseCase.java
public interface ManageDenomsUseCase {
    List<ProductDenoms> findByProductForAdmin(UUID productId);
    Optional<ProductDenoms> findById(UUID id);
    ProductDenoms create(UUID productId, CreateDenomRequest req);
    ProductDenoms update(UUID id, UpdateDenomRequest req);
    void softDelete(UUID id);
}
```

---

### 3. Request DTOs (Java Records)

**File baru** di `catalog/adapter/in/web/dto/`:

```java
// CreateCategoryRequest.java
public record CreateCategoryRequest(
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 100) String name,
    @NotNull CategoryType categoryType,
    String iconUrl,
    boolean active,
    int sortOrder
) {}

// UpdateCategoryRequest.java — sama dengan CreateCategoryRequest (code bisa diupdate)

// CreateProductRequest.java
public record CreateProductRequest(
    @NotNull UUID categoryId,
    @NotBlank @Size(max = 50) String code,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 100) String providerName,
    String description,
    String iconUrl,
    boolean active,
    int sortOrder
) {}

// UpdateProductRequest.java — sama, categoryId bisa diubah (pindah kategori)

// CreateDenomRequest.java
public record CreateDenomRequest(
    @NotBlank @Size(max = 100) String code,
    @NotBlank @Size(max = 150) String name,
    @NotNull DenomType denomType,
    BigDecimal nominal,
    @NotNull BigDecimal price,
    BigDecimal basePrice,
    BigDecimal adminFee,
    Integer validityDays,
    Long quotaMb,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    boolean requiresInquiry,
    Integer stockAvailable,
    boolean active,
    int sortOrder
) {}

// UpdateDenomRequest.java — sama
```

---

### 4. Domain Service Extensions

**Extend existing services** — tidak buat service baru (avoid duplication).

#### CategoryDomainService — tambah ManageCategoriesUseCase:

```java
@Service
@Transactional(readOnly = true)
public class CategoryDomainService implements BrowseCategoriesUseCase, ManageCategoriesUseCase {

    @Override
    public List<Categories> findAllForAdmin() {
        return categoryRepository.findAllByOrderBySortOrder(); // no active/deleted filter
    }

    @Override
    public Optional<Categories> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categoriesAll", "categoriesByType"}, allEntries = true)
    public Categories create(CreateCategoryRequest req) {
        // Validate code uniqueness
        if (categoryRepository.findByCode(req.code()).isPresent()) {
            throw new BusinessException("Category code already exists: " + req.code());
        }
        Categories cat = new Categories();
        cat.setCode(req.code().toUpperCase().trim());
        cat.setName(req.name());
        cat.setCategoryType(req.categoryType());
        cat.setIconUrl(req.iconUrl());
        cat.setActive(req.active());
        cat.setSortOrder(req.sortOrder());
        cat.setDeleted(false);
        return categoryRepository.save(cat);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categoriesAll", "categoriesByType"}, allEntries = true)
    public Categories update(UUID id, UpdateCategoryRequest req) {
        Categories cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        // Check code uniqueness (exclude self)
        if (categoryRepository.existsByCodeAndIdNot(req.code(), id)) {
            throw new BusinessException("Category code already exists: " + req.code());
        }
        cat.setCode(req.code().toUpperCase().trim());
        cat.setName(req.name());
        cat.setCategoryType(req.categoryType());
        cat.setIconUrl(req.iconUrl());
        cat.setActive(req.active());
        cat.setSortOrder(req.sortOrder());
        return categoryRepository.save(cat);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categoriesAll", "categoriesByType"}, allEntries = true)
    public void softDelete(UUID id) {
        Categories cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        cat.setDeleted(true);
        cat.setActive(false);
        categoryRepository.save(cat);
    }
}
```

> Pola yang sama untuk `ProductDomainService` dan `DenomDomainService`. `ProductDomainService.softDelete()` juga cascade soft-delete semua denoms milik product tersebut.

---

### 5. Security Constants

Tambah ke `OmniConstants.java`:

```java
// Admin Catalog Management — Keycloak realm roles
public static final String PERM_VIEW_CATALOG   = "REALM_view_catalog";
public static final String PERM_MANAGE_CATALOG = "REALM_manage_catalog";
```

> **Catatan keycloak**: Buat 2 realm roles ini di Keycloak dan assign ke admin user. Atau pakai existing `REALM_manage_roles` sementara jika belum ada. Dokumentasikan keputusan ini.

Tambah di `SecurityConfig.java`:
```java
.requestMatchers("/admin/catalog/**").authenticated()
.requestMatchers("/api/admin/catalog/**").authenticated()
```
> Fine-grained authorization via `@PreAuthorize` di controller — tidak perlu buat rule baru di SecurityConfig.

---

### 6. REST API Contract

**AdminCatalogController** — `@RestController @RequestMapping("/api/admin/catalog")`:

```
=== CATEGORIES ===
GET    /api/admin/catalog/categories          → List<CategoryDTO>  [PERM_VIEW_CATALOG]
GET    /api/admin/catalog/categories/{id}     → CategoryDTO        [PERM_VIEW_CATALOG]
POST   /api/admin/catalog/categories          → CategoryDTO 201    [PERM_MANAGE_CATALOG]
PUT    /api/admin/catalog/categories/{id}     → CategoryDTO        [PERM_MANAGE_CATALOG]
DELETE /api/admin/catalog/categories/{id}     → 204 No Content     [PERM_MANAGE_CATALOG]

=== PRODUCTS ===
GET    /api/admin/catalog/products            → List<ProductDTO>   [PERM_VIEW_CATALOG]
GET    /api/admin/catalog/products?categoryId={uuid} → filtered   [PERM_VIEW_CATALOG]
GET    /api/admin/catalog/products/{id}       → ProductDTO         [PERM_VIEW_CATALOG]
POST   /api/admin/catalog/products            → ProductDTO 201     [PERM_MANAGE_CATALOG]
PUT    /api/admin/catalog/products/{id}       → ProductDTO         [PERM_MANAGE_CATALOG]
DELETE /api/admin/catalog/products/{id}       → 204 No Content     [PERM_MANAGE_CATALOG] ← cascade delete denoms

=== DENOMS ===
GET    /api/admin/catalog/products/{productId}/denoms   → List<ProductDenomDTO>  [PERM_VIEW_CATALOG]
GET    /api/admin/catalog/denoms/{id}                   → ProductDenomDTO        [PERM_VIEW_CATALOG]
POST   /api/admin/catalog/products/{productId}/denoms   → ProductDenomDTO 201   [PERM_MANAGE_CATALOG]
PUT    /api/admin/catalog/denoms/{id}                   → ProductDenomDTO        [PERM_MANAGE_CATALOG]
DELETE /api/admin/catalog/denoms/{id}                   → 204 No Content         [PERM_MANAGE_CATALOG]
```

> Gunakan existing `CategoryDTO`, `ProductDTO`, `ProductDenomDTO` untuk response — tidak perlu DTO baru.

---

### 7. Page Controller + UI Routes

**AdminCatalogPageController** — `@Controller @RequestMapping("/admin/catalog")`:

```
GET /admin/catalog/categories          → pages/admin/catalog/categories.html
GET /admin/catalog/products            → pages/admin/catalog/products.html
GET /admin/catalog/products/{id}/denoms → pages/admin/catalog/denoms.html
```

Model attributes:
- `categories.html`: `categories` (List<CategoryDTO>), `categoryTypes` (CategoryType[])
- `products.html`: `products` (List<ProductDTO>), `categories` (for filter dropdown)
- `denoms.html`: `denoms` (List<ProductDenomDTO>), `product` (ProductDTO), `denomTypes` (DenomType[])

---

### 8. UI Template Structure

Pattern: ikuti `user-management.html` (table + filter + modal form + Alpine.js).

```
templates/pages/admin/catalog/
├── categories.html   — Table: code | name | type | active | actions (edit/delete)
│                        Modal: create/edit form
│                        Delete: confirm dialog
├── products.html     — Table: code | name | category | active | actions
│                        Filter: by category (dropdown)
│                        Modal: create/edit form (with category select)
└── denoms.html       — Table: code | name | price | nominal | type | active | actions
                         Header: bread-crumb (Category → Product → Denoms)
                         Modal: create/edit form (full denom fields)
```

Sidebar tambah menu "Catalog" di bawah Admin section (hardcoded, bukan dari Keycloak role attributes):
```html
<!-- sidebar.html — tambah sec:authorize -->
<a th:href="@{/admin/catalog/categories}" sec:authorize="hasRole('REALM_view_catalog')">
    Catalog Management
</a>
```

---

### 9. Edge Cases & Risk Register

| Case | Risk | Mitigasi |
|---|---|---|
| Code duplicate saat create | DB throws `DataIntegrityViolationException` | Validate di domain service sebelum save. Handler di `GlobalExceptionHandler` → 409 Conflict |
| Code duplicate saat update | Ganti code ke yang sudah dipakai entity lain | `existsByCodeAndIdNot()` check di update |
| Delete category yg masih ada products | Orphan products (category_id still valid) | Cascade: soft-delete semua products (dan denoms) ketika category di-delete. Atau: cek dulu, throw error jika ada product aktif |
| Delete product yg masih ada denoms | Orphan denoms | Domain service: loop soft-delete semua denoms sebelum soft-delete product |
| Delete denom yg sedang ada di Transactions | Data inconsistency | Denoms pakai soft-delete (bukan hard delete). Transactions tetap punya referensi valid. OK. |
| Concurrent update (2 admin edit bersamaan) | Lost update | `@Version` sudah ada di semua entities → `OptimisticLockException` → 409 Conflict |
| Cache stale setelah write | Reseller lihat data lama | `@CacheEvict` wajib di semua write operations |
| Denom price = 0 | Invalid data masuk DB | `@Positive` validation di `CreateDenomRequest.price` |
| Sort order kosong | Default ke 0, item numpuk | Di domain service: jika `sortOrder == 0`, set ke `currentMax + 10` (future improvement, optional) |

---

### 10. Task Breakdown untuk August

> **Kode seri**: `AP-` = **Admin Product** catalog management
> Ordered by dependency. Sequential — tiap task blocking task berikutnya.

| Task ID | Deskripsi | File(s) | Estimasi |
|---|---|---|---|
| **AP-1** | Extend port out: tambah `findById`, `findAllAdmin`, `existsByCodeAndIdNot` ke 3 repository ports | `CategoryRepositoryPort`, `ProductRepositoryPort`, `DenomRepositoryPort` | 20 mnt |
| **AP-2** | Verify JPA adapters compile setelah AP-1 (JpaRepository auto-satisfy port methods) | `CategoryJpaRepository`, `ProductJpaRepository`, `DenomJpaRepository` | 15 mnt |
| **AP-3** | Buat 3 use case interfaces + 6 request DTO records | `ManageCategories/Products/DenomsUseCase`, `Create/UpdateCategoryRequest`, `Create/UpdateProductRequest`, `Create/UpdateDenomRequest` | 30 mnt |
| **AP-4** | Extend `CategoryDomainService`: implements `ManageCategoriesUseCase` + `@CacheEvict` | `CategoryDomainService` | 30 mnt |
| **AP-5** | Extend `ProductDomainService`: implements `ManageProductsUseCase` + cascade softDelete denoms | `ProductDomainService` | 30 mnt |
| **AP-6** | Extend `DenomDomainService`: implements `ManageDenomsUseCase` | `DenomDomainService` | 25 mnt |
| **AP-7** | `OmniConstants`: 2 constants baru (`PERM_VIEW_CATALOG`, `PERM_MANAGE_CATALOG`) + `SecurityConfig`: 2 path rules | `OmniConstants`, `SecurityConfig` | 10 mnt |
| **AP-8** | `AdminCatalogController` — Category CRUD REST endpoints | `AdminCatalogController` | 30 mnt |
| **AP-9** | `AdminCatalogController` — Product + Denom CRUD REST endpoints | (lanjutan AP-8) | 30 mnt |
| **AP-10** | `AdminCatalogPageController` + 3 Thymeleaf templates (categories, products, denoms) | `AdminCatalogPageController`, `categories.html`, `products.html`, `denoms.html` | 90 mnt |
| **AP-11** | Sidebar link + `mvn clean package` verify (full build green) | `sidebar.html` | 10 mnt |

**Total estimasi: ~5.5 jam / 2-3 sesi**

**Session A (backend):** AP-1 → AP-2 → AP-3 → AP-4 → AP-5 → AP-6 → AP-7 → compile check
**Session B (frontend):** AP-8 → AP-9 → AP-10 → AP-11 → full test

---

**Neo's Warning:**
1. `CategoryJpaRepository extends JpaRepository<Categories, UUID>, CategoryRepositoryPort` — Setelah tambah `findById(UUID)` ke port, pastikan tidak conflict dengan JpaRepository yang juga punya `findById()`. Solution: **jangan tambah** `findById` ke port, karena JpaRepository sudah provide-nya. Domain service bisa langsung dapat dari JPA. Tapi ini melanggar hexagonal rule... **Decision**: Tambah tetap ke port (explicit contract), JpaRepository auto-implements via inheritance. Tidak ada conflict.

2. **Jangan** implement cache eviction di controller — harus di domain service layer. Cache adalah domain concern, bukan HTTP concern.

3. Denom `ProductDenoms.metadata` field adalah `@Transient` — **jangan include** dalam form submit. Admin form untuk denom tidak perlu edit metadata (kompleks, future feature).

---

**Last Updated**: 2026-03-02 (Neo — Admin Product Management Blueprint)
**Status**: DESIGN COMPLETE — Ready for August task breakdown + developer execution

---

---

## 🧭 Catalog Drill-down Navigation — Action Plan (Option A)

> **Neo Design**: 2026-03-02
> **Status**: READY FOR IMPLEMENTATION
> **Scope**: Ubah 3 halaman katalog jadi drill-down navigation (Categories → Products → Denoms)

---

### 0. Analisis Current State

**Sidebar**: Keycloak-driven, `view_catalog` role → `url=/admin/catalog/categories`, `display_name=Kelola Katalog`. ✅ Sudah 1 entry point. Tidak perlu diubah.

**Categories page** (`categories.html`):
- CRUD lengkap (Edit/Hapus per baris)
- ❌ Tidak ada navigasi ke Products dari sini — harus manually pergi ke halaman Products

**Products page** (`products.html`):
- Dropdown filter by category, tapi mulai dari "Semua Kategori"
- Nama produk sudah link ke `/admin/catalog/products/{id}/denoms` ✅
- Ada tombol "Denoms" per baris ✅
- ❌ Tidak ada breadcrumb
- ❌ Tidak bisa di-pre-filter dari URL param

**Denoms page** (`denoms.html`):
- Ada breadcrumb: `Kategori → Produk → {productName}`
- ❌ Breadcrumb link "Produk" pergi ke `/admin/catalog/products` tanpa filter — kehilangan konteks category

---

### 1. Desired UX Flow

```
Sidebar: "Kelola Katalog"
    ↓
┌─────────────────────────────┐
│ /admin/catalog/categories   │  Entry point
│ Tabel kategori + CRUD       │
│ [Produk →] per baris        │──────┐
└─────────────────────────────┘      │
                                     ↓
┌─────────────────────────────────────────┐
│ /admin/catalog/products?categoryId=xxx  │
│ Breadcrumb: Katalog > {CategoryName}    │
│ Tabel produk (pre-filtered) + CRUD      │
│ [Denom →] per baris                     │──────┐
└─────────────────────────────────────────┘      │
                                                 ↓
┌──────────────────────────────────────────────────────┐
│ /admin/catalog/products/{id}/denoms                  │
│ Breadcrumb: Katalog > {CategoryName} > {ProductName} │
│ Tabel denom + CRUD                                   │
└──────────────────────────────────────────────────────┘
```

---

### 2. Changes Per File

#### 2.1 `AdminCatalogPageController.java` — 2 perubahan

**A) Root redirect** — `/admin/catalog` → `/admin/catalog/categories`:
```java
@GetMapping
public String catalogRoot() {
    return "redirect:/admin/catalog/categories";
}
```

**B) `productsPage()` — terima `categoryId` + `categoryName` query params:**
```java
@GetMapping("/products")
public String productsPage(
        @RequestParam(required = false) String categoryId,
        @RequestParam(required = false) String categoryName,
        Model model) {
    model.addAttribute("currentPage", "admin-catalog");
    model.addAttribute("categoryId", categoryId != null ? categoryId : "");
    model.addAttribute("categoryName", categoryName != null ? categoryName : "");
    return "pages/admin/catalog/products";
}
```

> `denomsPage()` tidak perlu berubah — context category di-fetch via JS dari product API response.

---

#### 2.2 `categories.html` — 1 perubahan

Tambah tombol **"Produk →"** di kolom Aksi, sebelum tombol Edit:

```html
<a :href="'/admin/catalog/products?categoryId=' + cat.id + '&categoryName=' + encodeURIComponent(cat.name)"
   class="btn btn-ghost btn-xs" :disabled="cat.deleted">Produk →</a>
```

Lokasi: di dalam `<div class="flex justify-center gap-1">`, sebelum tombol Edit.

---

#### 2.3 `products.html` — 3 perubahan

**A) Tambah breadcrumb** (conditional, hanya muncul kalau dari drill-down):

Taruh sebelum Page Header:

```html
<!-- Breadcrumb (only when navigated from category drill-down) -->
<div th:if="${categoryName != null and !categoryName.isEmpty()}" class="text-sm breadcrumbs mb-4">
    <ul>
        <li><a href="/admin/catalog/categories">Katalog</a></li>
        <li class="font-semibold" th:text="${categoryName}">Nama Kategori</li>
    </ul>
</div>
```

**B) Inject `INITIAL_CATEGORY_ID` dari Thymeleaf ke JS:**

Di `<script>` block, tambah inline vars (ubah tag jadi `th:inline="javascript"`):
```html
<script th:inline="javascript">
    const INITIAL_CATEGORY_ID = /*[[${categoryId}]]*/ '';
</script>
```

**C) Pre-set `filterCategoryId` di Alpine.js:**

Dalam `productManager()`:
```js
filterCategoryId: INITIAL_CATEGORY_ID,  // was: ''
```

Tidak perlu perubahan lain — `loadProducts()` sudah pakai `filterCategoryId` untuk query param.

---

#### 2.4 `denoms.html` — 2 perubahan

**A) Extend `denomManager()` state + `loadProduct()` untuk capture category context:**

Tambah fields:
```js
categoryId: '',
categoryName: '',
```

Update `loadProduct()`:
```js
async loadProduct() {
    try {
        const res = await fetch(`/api/admin/catalog/products/${this.productId}`);
        if (res.ok) {
            const prod = await res.json();
            this.productName = prod.name;
            this.categoryId = prod.categoryId || '';
            this.categoryName = prod.categoryName || '';
        }
    } catch (e) { /* ignore */ }
},
```

**B) Fix breadcrumb** — ganti static links dengan dynamic Alpine.js bindings:

Replace existing breadcrumb:
```html
<!-- Before (static) -->
<li><a href="/admin/catalog/categories">Kategori</a></li>
<li><a href="/admin/catalog/products">Produk</a></li>
<li class="font-semibold" x-text="productName || 'Denominasi'"></li>
```

Menjadi:
```html
<!-- After (dynamic with category context) -->
<li><a href="/admin/catalog/categories">Katalog</a></li>
<li>
    <a :href="categoryId
        ? '/admin/catalog/products?categoryId=' + categoryId + '&categoryName=' + encodeURIComponent(categoryName)
        : '/admin/catalog/products'"
       x-text="categoryName || 'Produk'"></a>
</li>
<li class="font-semibold" x-text="productName || 'Denominasi'"></li>
```

---

### 3. File Inventory

| # | File | Perubahan | LOC estimate |
|---|---|---|---|
| 1 | `AdminCatalogPageController.java` | +root redirect, +2 `@RequestParam` | ~8 lines |
| 2 | `categories.html` | +1 drill-down link per row | ~3 lines |
| 3 | `products.html` | +breadcrumb, +JS vars, +pre-set filter | ~15 lines |
| 4 | `denoms.html` | +2 Alpine state fields, fix breadcrumb links | ~10 lines |

**Total: 4 file, ~36 lines changed. Tidak ada file baru.**

---

### 4. Tidak Perlu Diubah

- **Sidebar** (`sidebar.html`): Sudah benar — 1 entry point via Keycloak role.
- **AdminCatalogController.java** (REST API): Sudah support `?categoryId=` di product listing.
- **Backend** (domain services, ports): Zero changes.
- **Keycloak**: Zero changes.

---

### 5. Task Breakdown untuk August

> **Kode seri**: `AP-N` = **Admin Product Navigation** (drill-down)
> Semua bisa dilakukan dalam 1 sesi.

| Task ID | Deskripsi | File(s) | Estimasi |
|---|---|---|---|
| **AP-N1** | `AdminCatalogPageController` — root redirect + `@RequestParam` categoryId/categoryName di `productsPage()` | `AdminCatalogPageController.java` | 10 mnt |
| **AP-N2** | `categories.html` — tambah tombol "Produk →" per row di kolom Aksi | `categories.html` | 5 mnt |
| **AP-N3** | `products.html` — breadcrumb + JS `INITIAL_CATEGORY_ID` + pre-set `filterCategoryId` | `products.html` | 15 mnt |
| **AP-N4** | `denoms.html` — extend `loadProduct()` untuk category context + fix breadcrumb dynamic links | `denoms.html` | 15 mnt |
| **AP-N5** | Manual test: drill-down flow end-to-end + verify breadcrumb links + `mvn compile` | — | 10 mnt |

**Total estimasi: ~55 menit / 1 sesi**

---

### 6. Neo's Notes

1. **Query param `categoryName`**: Pragmatic approach — pass display name via URL param, bukan fetch ulang dari API. Trade-off: URL lebih panjang, tapi zero extra backend calls. Acceptable untuk admin UI.

2. **Breadcrumb di products.html conditional**: Hanya muncul kalau ada `categoryName`. Kalau user langsung akses `/admin/catalog/products` tanpa param (misal bookmark), halaman tetap berfungsi normal tanpa breadcrumb — graceful degradation.

3. **Denoms breadcrumb dynamic**: Category info di-fetch dari product API response (`ProductDTO.categoryId`, `ProductDTO.categoryName`). Sudah confirmed field ini ada di DTO. Tidak perlu API call tambahan.

4. **Tidak perlu `@RequestParam` di denoms page controller**: Product info (termasuk category) sudah di-fetch via Alpine.js `loadProduct()`. Menambah param di server-side controller hanya duplikasi.

---

---

## 🔐 Mandatory Role Assignment — Technical Blueprint

> **Neo Design**: 2026-03-03
> **Status**: READY FOR IMPLEMENTATION
> **Scope**: Pastikan setiap user yang dibuat di SatSetGo punya minimal 1 role — zero orphan users
> **Effort**: ~1 sesi (4 file, ~30 LOC perubahan + fix bug)

### Context

Julia requirement (2026-03-03): "Zero user tanpa role." Tiga alur pembuatan user:

| Path | Alur | Role | Status |
|------|------|------|--------|
| A | Self-service onboarding | `org_owner` (client) auto-assign | ✅ Already implemented |
| B | Admin create org/reseller | `org_owner` (client) auto-assign | ✅ Already implemented |
| C | Admin create backoffice user | Realm role mandatory input | ❌ **Needs fix** |

**Path A & B sudah benar** — `StoreOnboardingDomainService` dan `AdminOnboardingDomainService` sudah panggil `assignClientRoleToUser(userId, "org_owner")`. Tidak ada perubahan.

**Path C punya 3 masalah:**

### Problem Analysis (Path C)

#### P1 — DTO tanpa validasi
```java
// CreateUserRequest.java
private List<String> roles;  // NO @NotNull, NO @NotEmpty
// Comment: "Can be empty if no roles should be assigned" ← SALAH, harus mandatory
```

#### P2 — Frontend "Skip" button
```html
<!-- user-management.html Step 2 -->
<button @click="skipRoles()">Skip</button>  <!-- Bisa bikin user tanpa role -->
```
`skipRoles()` set `selectedRoles = []` lalu langsung `submitForm()`.

#### P3 — Service crash on empty roles
```java
// IdentityDomainService.java:141
reqUserDTO.getRoles().getFirst();  // NoSuchElementException jika roles = []
                                   // NullPointerException jika roles = null
```
Tidak di-catch oleh `catch (BusinessException e)` → HTTP 500.

### Implementation Plan

#### MR-1: Validate `roles` as mandatory in DTO
**File**: `src/main/java/com/omnip/identity/adapter/in/web/dto/CreateUserRequest.java`
**Change**:
```java
// BEFORE:
private List<String> roles;

// AFTER:
@NotEmpty(message = "Minimal 1 role harus dipilih")
private List<String> roles;
```
**Impact**: Request tanpa roles → HTTP 400 Bad Request (Bean Validation), bukan 500.

#### MR-2: Support multi-role assignment at creation
**File**: `src/main/java/com/omnip/identity/domain/service/IdentityDomainService.java`
**Change**: Ganti `.getFirst()` → loop semua roles:
```java
// BEFORE:
String providerUserId = keycloakAdminClientService.createBackofficeUser(
    ..., reqUserDTO.getRoles().getFirst());

// AFTER:
// Create user dulu (tanpa role di method createBackofficeUser)
// Lalu assign semua roles via loop
String providerUserId = keycloakAdminClientService.createBackofficeUser(
    reqUserDTO.getUsername(), reqUserDTO.getFullname(),
    reqUserDTO.getEmail(), reqUserDTO.getPassword(),
    reqUserDTO.getRoles().getFirst());

// Assign additional roles (index 1+)
for (int i = 1; i < reqUserDTO.getRoles().size(); i++) {
    keycloakAdminClientService.assignRoleToUser(
        providerUserId, reqUserDTO.getRoles().get(i));
}
```
**Rationale**: Admin mungkin perlu assign >1 realm role sekaligus (contoh: `view_users` + `manage_users`). `createBackofficeUser()` sudah handle role pertama, sisanya via `assignRoleToUser()`.

#### MR-3: Remove "Skip" button, enforce role selection in form
**File**: `src/main/resources/templates/pages/admin/user-management.html`
**Changes**:
1. **Hapus tombol "Skip"** (`skipRoles()` function call)
2. **Tambah validasi di `submitForm()`**: Block submit jika `selectedRoles.length === 0`
3. **Set `errors.roles`** jika tidak ada role terpilih:
```javascript
// Di submitForm() atau validateStep2():
if (this.selectedRoles.length === 0) {
    this.errors.roles = 'Minimal 1 role harus dipilih';
    return;
}
```
4. **Disable tombol "Buat User"** saat `selectedRoles.length === 0` (visual feedback)

#### MR-4: Update comment di DTO
**File**: `CreateUserRequest.java`
**Change**: Update Javadoc comment:
```java
// BEFORE: "Can be empty if no roles should be assigned."
// AFTER: "At least one role is required for backoffice users."
```

### File Impact Summary

| # | File | Change | LOC |
|---|------|--------|-----|
| MR-1 | `CreateUserRequest.java` | Add `@NotEmpty` + update comment | ~3 |
| MR-2 | `IdentityDomainService.java` | Multi-role loop, fix crash | ~8 |
| MR-3 | `user-management.html` | Remove Skip, add validation | ~15 |
| MR-4 | `CreateUserRequest.java` | Update comment (merged with MR-1) | — |
| **Total** | **3 files** | | **~26 LOC** |

### Execution Order

```
MR-1 (DTO validation)  ──→  MR-2 (service fix)  ──→  MR-3 (frontend)  ──→  mvn compile verify
```

Sequential — MR-1 protects the backend, MR-2 fixes the crash, MR-3 fixes the UI.

### Edge Cases

| Case | Expected Behavior |
|------|-------------------|
| API call tanpa `roles` field | HTTP 400 — `@NotEmpty` validation |
| API call dengan `roles: []` | HTTP 400 — `@NotEmpty` validation |
| API call dengan `roles: ["nonexistent_role"]` | Keycloak throws → `BusinessException` → handled |
| Form submit tanpa pilih role | Button disabled + error message |
| Existing users tanpa role | **OUT OF SCOPE** — separate migration task |

### Out of Scope (Conscious Decision)

1. **Existing user migration** — cek dan fix user lama yang tidak punya role. Ini butuh audit Keycloak + DB, beda task.
2. **Path A & B changes** — sudah benar, tidak disentuh.
3. **Role hierarchy changes** — `org_owner` → `org_operator` → `transaction` tetap.

**Last Updated**: 2026-03-03 (Neo — Mandatory Role Assignment Blueprint)

---

---

## 💰 Wallet Service — Technical Design

> **Neo Design**: 2026-03-05
> **Status**: PLANNING — belum dieksekusi. Menunggu keputusan repo structure dari Kokoh.
> **Scope**: Pisahkan semua urusan finansial ke service terpisah `omnip-wallet`

---

### 1. Architecture Overview

```
┌─────────────────────────────────┐     ┌──────────────────────────────────┐
│   omnip-services-3 (Core)       │     │   omnip-wallet (Wallet Service)  │
│                                 │     │                                  │
│  - Catalog (produk, denom)      │────>│  - Balance management            │
│  - Transactions (flow)          │REST │  - Mutation ledger (Double-Entry) │
│  - Onboarding (store, user)     │     │  - Top-up processing             │
│  - User management              │     │  - Refund operations             │
│  - UI (Thymeleaf)               │     │  - Mutation history              │
└─────────────────────────────────┘     └──────────────────────────────────┘
              │                                        │
              └──────────────────┬─────────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │       Keycloak          │
                    │     (satset-go)         │
                    │   Shared Auth Realm     │
                    └─────────────────────────┘
```

---

### 2. Data Ownership

| Data | Current Location | After Separation |
|------|-----------------|------------------|
| `store_mutations` table | Core DB (`satset_go`) | **Wallet DB** (terpisah) |
| `stores.balance` field | Core DB (`Stores` entity) | **Deprecated** — Wallet = single source of truth |
| `Transactions` table | Core DB | Tetap di Core |
| Payment gateway callbacks | (belum ada) | **Wallet Service** — bukan Core |
| Store entity | Core DB | Tetap di Core — wallet hanya kenal `storeId` (UUID) |

**Key Decision**: Wallet tidak kenal `Stores` entity. Hanya pakai `storeId` (UUID) sebagai identifier. Ini clean boundary.

---

### 3. Wallet Service — Internal API Contract

```
Base URL: /internal/wallet

GET  /internal/wallet/balance/{storeId}
     → { storeId, balance, currency: "IDR", asOf: timestamp }

POST /internal/wallet/debit
     Body: { storeId, amount, referenceId, referenceType, description }
     → { mutationId, newBalance }

POST /internal/wallet/credit
     Body: { storeId, amount, referenceId, referenceType, description }
     → { mutationId, newBalance }

POST /internal/wallet/refund
     Body: { storeId, amount, originalReferenceId, description }
     → { mutationId, newBalance }

GET  /internal/wallet/mutations/{storeId}?page=0&size=20
     → Page<MutationDTO>
```

**Auth**: JWT Bearer token. Wallet = Keycloak Resource Server (realm `satset-go`, issuer same). Core call wallet via **service account** (client credentials grant — bukan atas nama user).

---

### 4. Distributed Transaction — Saga Pattern

Masalah utama pemisahan: `TransactionDomainService` saat ini melakukan atomic:
```
1. deductBalance()  →  2. callProvider()  →  3. (if fail) refundBalance()
```

Dalam 1 DB transaction. Setelah pemisahan, ini tidak bisa atomic lagi.

**Solusi: Saga Choreography dengan Idempotent Operations**

```
Core: POST /api/purchase
  Step 1: POST /internal/wallet/debit  { storeId, amount, referenceId: txId }
          → OK (balance deducted)
  Step 2: Core calls MockProvider/RealProvider
          → SUCCESS → Core marks Transaction status = SUCCESS
          → FAIL    → Step 3
  Step 3 (compensating): POST /internal/wallet/refund { storeId, amount, originalReferenceId: txId }
          → Balance restored (compensating mutation appended to ledger)
```

**Idempotency**: Setiap operation harus idempotent via `referenceId`. Kalau Core retry debit dengan `referenceId` yang sama → Wallet check: sudah ada → return existing result tanpa debit ulang.

```sql
-- Wallet DB: unique constraint
ALTER TABLE store_mutations ADD CONSTRAINT uq_mutations_reference
    UNIQUE (reference_id, reference_type, mutation_type);
```

---

### 5. Auth Integration (Keycloak)

```yaml
# omnip-wallet/src/main/resources/application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_BASE_URL}/realms/${KEYCLOAK_REALM}
          # Same realm as core — shared JWKS endpoint
```

Core memanggil Wallet pakai **client credentials**:
```java
// Core: WalletClient.java (RestClient atau WebClient)
// Ambil token dari Keycloak dengan client_credentials grant
// Authorization: Bearer <service_account_token>
```

Wallet perlu validasi bahwa caller adalah service account (bukan end user) — via claim `azp` atau custom scope.

---

### 6. Migration Path dari Current Implementation

| Phase | Action | Risk |
|-------|--------|------|
| **Phase 0** | Design finalized, repo created | Low |
| **Phase 1** | Wallet service skeleton (Spring Boot, Keycloak auth, basic endpoints) | Low |
| **Phase 2** | Migrate `StoreMutations` entity → Wallet DB | Medium — data migration needed |
| **Phase 3** | `BalanceDomainService` logic → `WalletDomainService` di wallet service | Medium |
| **Phase 4** | Core: replace `BalanceDomainService` calls → HTTP calls ke Wallet API | High — distributed tx |
| **Phase 5** | Deprecated `Stores.balance` field (nullable, eventually removed) | Low |
| **Phase 6** | Integration tests end-to-end (purchase flow with Saga) | Medium |

---

### 7. Wallet Service — Tech Stack (Proposed)

| Concern | Decision | Rationale |
|---------|----------|-----------|
| Framework | Spring Boot 4.0.1 (sama) | Consistency, same team expertise |
| Java | 25 (sama) | Virtual threads, same version |
| Database | PostgreSQL (terpisah dari core) | Domain isolation |
| Auth | Keycloak Resource Server | Same realm, zero new auth infra |
| API style | REST (internal) | Simpel, debuggable, team familiar |
| Communication | Sync REST (short-term) | Async event-driven bisa ditambah nanti |

---

### 8. Decisions (Resolved 2026-03-05)

| # | Pertanyaan | Keputusan |
|---|-----------|-----------|
| 1 | **Repo structure?** | ✅ **Multi-module Maven** dalam 1 repo `omnip-services-3` |
| 2 | **Database?** | Schema terpisah (`wallet_accounts`, `wallet_mutations`) di DB yang sama untuk MVP. Migrate ke DB terpisah kalau perlu scale. |
| 3 | **Timeline eksekusi?** | WR-series dulu (di Core), lalu W-series (Wallet module baru) |

---

### 9. Multi-Module Maven — Structure & Migration Plan

**Target Structure**:
```
omnip-services-3/
├── pom.xml              ← Parent POM (packaging: pom)
│                           inherits spring-boot-starter-parent 4.0.1
│                           manages versions untuk semua child modules
├── omnip-core/          ← Current app (code dipindah ke sini)
│   ├── pom.xml          ← Spring Boot app, parent: omnip-services-3
│   └── src/             ← semua code yang sekarang ada di root/src/
└── omnip-wallet/        ← NEW wallet service
    ├── pom.xml          ← Spring Boot app, parent: omnip-services-3
    └── src/
```

**Parent POM** (`omnip-services-3/pom.xml`):
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.1</version>
</parent>
<groupId>com.omnip</groupId>
<artifactId>omnip-platform</artifactId>
<version>0.0.1-SNAPSHOT</version>
<packaging>pom</packaging>

<modules>
    <module>omnip-core</module>
    <module>omnip-wallet</module>
</modules>

<properties>
    <java.version>25</java.version>
    <!-- Semua version overrides dipindah ke sini dari omnip-core/pom.xml -->
</properties>
```

**Core POM** (`omnip-core/pom.xml`):
```xml
<parent>
    <groupId>com.omnip</groupId>
    <artifactId>omnip-platform</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>
<artifactId>omnip-core</artifactId>
<packaging>jar</packaging>

<!-- Semua <dependencies> dan <build> dari pom.xml yang sekarang dipindah ke sini -->
```

**Wallet POM** (`omnip-wallet/pom.xml`):
```xml
<parent>
    <groupId>com.omnip</groupId>
    <artifactId>omnip-platform</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</parent>
<artifactId>omnip-wallet</artifactId>
<packaging>jar</packaging>

<dependencies>
    <dependency>spring-boot-starter-web</dependency>
    <dependency>spring-boot-starter-data-jpa</dependency>
    <dependency>spring-boot-starter-oauth2-resource-server</dependency>
    <dependency>postgresql</dependency>
    <dependency>lombok</dependency>
</dependencies>
```

**Migration Steps (W-SETUP)**:
```bash
# 1. Buat direktori
mkdir -p omnip-core omnip-wallet/src/main/java/com/omnip omnip-wallet/src/main/resources

# 2. Pindah code (git mv — tercatat di history)
git mv src omnip-core/src

# 3. Pindah config files
git mv .mvn omnip-core/.mvn   # kalau ada

# 4. Update root pom.xml → parent POM
# 5. Buat omnip-core/pom.xml (inherit dari parent, deps dari root pom.xml lama)
# 6. Buat omnip-wallet/pom.xml skeleton

# 7. Verify
cd omnip-core && mvn compile   # harus sukses, zero code changes
```

**Yang TIDAK berubah setelah migration**:
- Semua Java packages (`com.omnip.*`) — tetap
- Semua source files — tetap, hanya path directory berubah
- Database config, Keycloak config — tetap (di `omnip-core/src/main/resources/`)
- Git history — `git mv` preserve history

**Yang berubah**:
- `mvn spring-boot:run` → dijalankan dari `omnip-core/` directory
- Build command dari root: `mvn clean package` — build keduanya
- IDE project structure — perlu re-import sebagai multi-module project

---

**Last Updated**: 2026-03-05 (Neo — Wallet Service Technical Design)
**Status**: DESIGN DRAFT — Menunggu open questions resolved sebelum W-series execution

---

---

## 🔧 Wallet Refactor — WR-series (Preparation dalam Core)

> **Neo Design**: 2026-03-05
> **Status**: READY FOR IMPLEMENTATION — tidak butuh keputusan repo dulu
> **Prerequisite**: Harus selesai sebelum W-series (Wallet extraction)
> **Goal**: Bersihkan domain model debit/kredit di Core agar siap di-extract ke Wallet service
> **Risk**: Medium — ada DB migration (`wallet_accounts` table baru), tapi zero functional change
> **Effort**: ~1 sesi (5 file baru, 6 file dimodifikasi)

---

### Context: Masalah yang Harus Dibersihkan

```
SEKARANG (kotor):
StoreMutations ──@ManyToOne──> Stores          ← C-2 violation: cross-context JPA FK
BalanceDomainService           ──inject──> StoreBalancePort (return Stores entity)
TransactionDomainService       ──inject──> BalanceDomainService (concrete, bukan port)
BalanceManagementUseCase.deductBalance()  → return StoreMutations (entity bocor di port)
Stores.balance                 ← finansial data numpang di onboarding entity

SETELAH REFACTOR (bersih):
WalletMutation ──UUID storeId──> (no JPA relation) ← clean boundary
BalanceDomainService           ──inject──> WalletAccountPort (return WalletAccount)
TransactionDomainService       ──inject──> BalanceManagementUseCase (port, bukan concrete)
BalanceManagementUseCase.deductBalance()  → return MutationResult (domain record)
WalletAccount                  ← entity sendiri: { storeId, balance } — siap dipindah ke wallet service
```

---

### WR-1: Introduce `WalletAccount` Entity

**File baru**: `transaction/domain/model/WalletAccount.java`

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class WalletAccount {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "store_id", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID storeId;                          // UUID saja, bukan @ManyToOne Stores

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version;                          // optimistic lock untuk general update
}
```

**Table baru**: `wallet_accounts` — Hibernate DDL auto-create karena `ddl-auto: update`.

**Data migration** (dijalankan sekali via `DataSeeder` atau SQL script):
```sql
INSERT INTO wallet_accounts (id, store_id, balance, created_at, updated_at, version)
SELECT gen_random_uuid(), id, COALESCE(balance, 0), created_at, updated_at, 0
FROM stores
ON CONFLICT (store_id) DO NOTHING;
```

---

### WR-2: Introduce `WalletAccountPort` (Port Out)

**File baru**: `transaction/domain/port/out/WalletAccountPort.java`

```java
public interface WalletAccountPort {

    Optional<WalletAccount> findByStoreId(UUID storeId);

    // Pessimistic lock — untuk operasi finansial (debit/credit)
    Optional<WalletAccount> findByStoreIdWithLock(UUID storeId);

    WalletAccount save(WalletAccount account);
}
```

**File baru**: `transaction/adapter/out/persistence/WalletAccountJpaRepository.java`

```java
@Repository
public interface WalletAccountJpaRepository
        extends JpaRepository<WalletAccount, UUID>, WalletAccountPort {

    Optional<WalletAccount> findByStoreId(UUID storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletAccount w WHERE w.storeId = :storeId")
    Optional<WalletAccount> findByStoreIdWithLock(@Param("storeId") UUID storeId);
}
```

---

### WR-3: Introduce `MutationResult` Domain Record (Port Return Type)

**File baru**: `transaction/domain/model/MutationResult.java`

```java
// Domain record — bukan entity. Aman bocor ke port/controller.
public record MutationResult(
    UUID mutationId,
    BigDecimal balanceAfter
) {}
```

**Kenapa**: `BalanceManagementUseCase` sekarang return `StoreMutations` entity — entity tidak boleh bocor keluar domain service. `MutationResult` adalah clean return type yang bisa dipakai caller tanpa coupling ke persistence.

---

### WR-4: Fix `StoreMutations` — Hapus `@ManyToOne Stores`

**File dimodifikasi**: `transaction/domain/model/StoreMutations.java`

```java
// BEFORE:
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "store_id", nullable = false)
private Stores store;

// AFTER:
@Column(name = "store_id", columnDefinition = "uuid", nullable = false)
private UUID storeId;
```

**DB impact**: Kolom `store_id` sudah ada (FK sebelumnya). Hibernate DDL `update` akan drop constraint FK, kolom tetap ada. Data tidak hilang.

**Idempotency constraint** (tambah sekaligus):
```java
// Tambah ke StoreMutations untuk support idempotency di future Saga:
@Column(name = "reference_id", columnDefinition = "uuid")
private UUID referenceId;        // sudah ada ✅

// Unique constraint untuk idempotency (DEBIT dengan referenceId yang sama = tolak duplikat):
// @Table(uniqueConstraints = @UniqueConstraint(columnNames = {"reference_id", "reference_type", "type"}))
// Catatan: tambahkan ini di class level @Table annotation
```

---

### WR-5: Fix `BalanceManagementUseCase` Port

**File dimodifikasi**: `transaction/domain/port/in/BalanceManagementUseCase.java`

```java
// BEFORE:
StoreMutations deductBalance(UUID storeId, BigDecimal amount,
        MutationReferenceType referenceType, UUID referenceId, String description)
        throws InsufficientBalanceException;

StoreMutations addBalance(UUID storeId, BigDecimal amount,
        MutationReferenceType referenceType, UUID referenceId, String description);

// AFTER:
MutationResult deductBalance(UUID storeId, BigDecimal amount,
        MutationReferenceType referenceType, UUID referenceId, String description)
        throws InsufficientBalanceException;

MutationResult addBalance(UUID storeId, BigDecimal amount,
        MutationReferenceType referenceType, UUID referenceId, String description);

BigDecimal getBalance(UUID storeId);   // tetap sama
```

---

### WR-6: Refactor `BalanceDomainService`

**File dimodifikasi**: `transaction/domain/service/BalanceDomainService.java`

```java
@Service
public class BalanceDomainService implements BalanceManagementUseCase {

    private final WalletAccountPort walletAccountPort;      // ← ganti StoreBalancePort
    private final StoreMutationRepositoryPort mutationRepo;

    @Override
    @Transactional
    public MutationResult deductBalance(UUID storeId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description) {

        WalletAccount account = walletAccountPort.findByStoreIdWithLock(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", storeId));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(...);
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);

        StoreMutations mutation = buildMutation(storeId, amount, MutationType.DEBIT,
                newBalance, referenceType, referenceId, description);
        mutation = mutationRepo.save(mutation);

        account.setBalance(newBalance);
        walletAccountPort.save(account);

        return new MutationResult(mutation.getId(), newBalance);
    }

    @Override
    @Transactional
    public MutationResult addBalance(UUID storeId, BigDecimal amount, ...) {
        WalletAccount account = walletAccountPort.findByStoreIdWithLock(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", storeId));

        BigDecimal newBalance = account.getBalance().add(amount);

        StoreMutations mutation = buildMutation(storeId, amount, MutationType.CREDIT,
                newBalance, referenceType, referenceId, description);
        mutation = mutationRepo.save(mutation);

        account.setBalance(newBalance);
        walletAccountPort.save(account);

        return new MutationResult(mutation.getId(), newBalance);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID storeId) {
        return walletAccountPort.findByStoreId(storeId)
                .map(WalletAccount::getBalance)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", storeId));
    }

    private StoreMutations buildMutation(UUID storeId, BigDecimal amount,
            MutationType type, BigDecimal balanceAfter,
            MutationReferenceType refType, UUID refId, String description) {
        StoreMutations m = new StoreMutations();
        m.setStoreId(storeId);         // UUID, bukan Stores entity
        m.setAmount(amount);
        m.setType(type);
        m.setBalanceAfter(balanceAfter);
        m.setReferenceType(refType);
        m.setReferenceId(refId);
        m.setDescription(description);
        return m;
    }
}
```

---

### WR-7: Fix `TransactionDomainService` — Inject Port, Hapus `StoreBalancePort`

**File dimodifikasi**: `transaction/domain/service/TransactionDomainService.java`

```java
// BEFORE:
private final BalanceDomainService balanceService;       // concrete class
private final StoreBalancePort storeRepository;          // untuk balance check

// AFTER:
private final BalanceManagementUseCase balanceService;   // inject port (interface)
// StoreBalancePort dihapus dari TransactionDomainService
// (balance check sudah di BalanceDomainService.deductBalance() via InsufficientBalanceException)
```

`topUp()` dan `createPurchase()` tidak berubah secara logic — hanya signature `balanceService` berubah dari concrete ke interface. Return type `deductBalance/addBalance` berubah dari `StoreMutations` ke `MutationResult`, tapi `TransactionDomainService` tidak pernah pakai return value-nya → **zero change di call site**.

---

### WR-8: Deprecate `Stores.balance`

**File dimodifikasi**: `onboarding/domain/model/Stores.java`

```java
// BEFORE:
private BigDecimal balance = BigDecimal.ZERO;

// AFTER:
@Deprecated(since = "wallet-refactor", forRemoval = true)
@Column(nullable = true)   // nullable — tidak lagi jadi source of truth
private BigDecimal balance;
```

Field tidak dihapus sekarang — tunggu `WalletAccount` stable dan data migration selesai. Hapus di W-series ketika Core tidak lagi reference `Stores.balance`.

---

### File Impact Summary

| ID | File | Action | LOC change |
|----|------|--------|-----------|
| WR-1 | `WalletAccount.java` | **Baru** | ~40 |
| WR-2a | `WalletAccountPort.java` | **Baru** | ~10 |
| WR-2b | `WalletAccountJpaRepository.java` | **Baru** | ~15 |
| WR-3 | `MutationResult.java` | **Baru** | ~5 |
| WR-4 | `StoreMutations.java` | Modify — hapus `@ManyToOne`, ganti `UUID storeId` | ~8 |
| WR-5 | `BalanceManagementUseCase.java` | Modify — return type → `MutationResult` | ~6 |
| WR-6 | `BalanceDomainService.java` | Modify — inject `WalletAccountPort`, refactor logic | ~30 |
| WR-7 | `TransactionDomainService.java` | Modify — inject port, hapus `StoreBalancePort` | ~5 |
| WR-8 | `Stores.java` | Modify — `@Deprecated balance` | ~3 |
| WR-9 | `StoreBalancePort.java` | **Delete** (setelah WR-7 tidak ada consumer) | -20 |
| WR-10 | `DataSeeder.java` | Modify — seed `WalletAccount` dari existing `Stores.balance` | ~15 |
| WR-11 | `TransactionDomainServiceTest.java` | Modify — mock `BalanceManagementUseCase` (port), update return type | ~10 |
| WR-12 | `PurchaseFlowIntegrationTest.java` | Modify — mock `WalletAccountPort`, remove `StoreBalancePort` mocks | ~10 |

**Total**: 5 file baru, 7 file dimodifikasi, 1 file dihapus. ~177 LOC net.

---

### Execution Order

```
WR-1 (WalletAccount entity)
  → WR-2 (WalletAccountPort + JpaRepository)
    → WR-3 (MutationResult record)
      → WR-4 (StoreMutations fix)
        → WR-5 (BalanceManagementUseCase fix)
          → WR-6 (BalanceDomainService refactor)
            → WR-7 (TransactionDomainService fix)
              → WR-8 (Stores.balance deprecate)
                → WR-9 (StoreBalancePort delete)
                  → WR-10 (DataSeeder migration)
                    → WR-11 + WR-12 (fix tests)
                      → mvn clean package (BUILD SUCCESS, semua tests pass)
```

---

### Neo's Critical Notes

1. **`StoreBalancePort` masih dipakai oleh `StoreJpaRepository`** — cek consumer sebelum delete WR-9. Jalankan `grep -r "StoreBalancePort"` sebelum hapus.

2. **DB migration `wallet_accounts`** — `DataSeeder` harus idempotent (sudah ada pattern dari L-6). Gunakan `findByStoreId().orElseGet()` pattern yang sama.

3. **`StoreMutations.store` → `storeId`**: Kolom DB-nya sudah `store_id`. Setelah hapus `@ManyToOne`, Hibernate akan drop FK constraint saja — kolom data tetap ada. Aman.

4. **`TransactionDomainService` tidak pakai return value dari `deductBalance/addBalance`** — konfirmasi di kode: `balanceService.deductBalance(...)` tanpa assignment. Berarti WR-5 (ubah return type) tidak break call site. ✅

5. **Setelah WR-series selesai**, `StoreMutations` dan `WalletAccount` siap dipindah ke Wallet service (W-series) dengan zero structural changes — tinggal pindah package.

---

**Last Updated**: 2026-03-05 (Neo — WR-series Wallet Refactor Blueprint)
**Status**: READY — bisa dikerjakan tanpa menunggu keputusan repo Wallet service
