# SatSetGo - Technical Specifications

> **Owner**: Neo (Chief Technical Architect)
> **Last Updated**: 2026-02-20
> **Current Sprint**: Prerequisite — Store Onboarding + Keycloak Organization

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
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://prod-host:5432/satsetgo
DB_USERNAME=satsetgo_user
DB_PASSWORD=<strong-password>
KEYCLOAK_ISSUER_URI=https://auth.satsetgo.com/realms/satsetgo
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

### 🟠 High — Architecture Violations & Security

| ID | Issue | File(s) | Impact |
|---|---|---|---|
| **H-1** | `shared` package imports JPA adapter repos directly | `Beans.java`, `StoreOnboardingInterceptor.java`, `DataSeeder.java` | Shared kernel coupled to specific adapter layer |
| **H-3** | Port interfaces reference adapter-layer DTOs & Keycloak SDK (`UserRepresentation`) | `KeycloakIdentityPort.java`, `ManageRolesUseCase.java`, `ManageMyProfileUseCase.java` | Inward dependency violation |
| **H-6** | `UserDTO` holds `Stores` JPA entity reference | `UserDTO.java:31` | `LazyInitializationException` risk in session serialization |

### 🟡 Medium — Correctness & Fragility

| ID | Issue | File(s) |
|---|---|---|
| **M-1** | `Transactions` entity missing `@Version` — race condition on status update | `Transactions.java` |
| **M-2** | `StoreMutations` entity missing `@Version` (append-only, but inconsistent with pattern) | `StoreMutations.java` |
| **M-3** | Mixed pessimistic + optimistic locking on `Stores.balance` | `Stores.java`, `BalanceDomainService.java` |
| **M-4** | `RegistrationHelper` uses `java.util.Random` (not `SecureRandom`) for referral IDs | `RegistrationHelper.java:24` |
| **M-5** | Role prefix double-prefix: `ROLE_REALM_` works by coincidence | `SecurityConfig.java:135` |
| **M-6** | Tomcat `max-threads: 200` irrelevant with virtual threads enabled | `application.yml:77-81` |
| **M-7** | `ProductDenoms.metadata` `@Transient` field — null in all paths except `getDenomWithMeta()` | `ProductDenoms.java:93-94` |
| **M-8** | Cache name collision: `findAll()` and `findByType()` share cache `"categories"` | `CategoryDomainService.java` |
| **M-9** | `StoreMutationJpaRepository.findTopBy...` accepts `Stores` entity instead of `UUID` | `StoreMutationJpaRepository.java:15` |
| **M-10** | Hardcoded `keycloak.realm: satset-go` in default config (no env variable) | `application.yml:66` |

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
