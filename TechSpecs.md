# SatSetGo - Technical Specifications

> **Owner**: Neo (Chief Technical Architect)
> **Last Updated**: 2026-02-12
> **Current Sprint**: Week 2 - Purchase Prepaid Flow

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

**Current Debt**:
- No unit tests (planned Week 4)
- `Stores.createdDate` uses `java.util.Date` (inconsistent with other entities)
- No pagination on product listing (acceptable until 100+ products)

**Monitoring**:
- Review debt every sprint
- Address when it blocks new features or causes bugs

---

## 🎯 Week 2 Technical Specifications

*(This section will be populated when Neo is activated for Week 2 design)*

**Status**: Pending Neo activation

---

**Last Updated**: 2026-02-12
**Next Review**: When Neo is called for architecture design
