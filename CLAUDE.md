# PPOB Server - Project Context

**Project**: PPOB (Payment Point Online Bank) Server - Multi-product digital marketplace
**Type**: Spring Boot REST API + Thymeleaf UI
**Approach**: Hybrid Feature Slicing (vertical slices with shared infrastructure)
**Roadmap**: `ROADMAP.md` | **Tasks**: `Tasks.md`

---

## Tech Stack
- **Framework**: Spring Boot 4.0.1 | **Java**: 25 | **Build**: Maven
- **Database**: PostgreSQL | **ORM**: Hibernate 7.x (JPA)
- **Auth**: Keycloak (OAuth2/OIDC) | **Cache**: Caffeine
- **UI**: Thymeleaf + Tailwind CSS

## Database Schema
```
Categories (PULSA, DATA, GAME, PLN_POSTPAID)
  ↓ 1:N
Products (TELKOMSEL, XL, GARENA, PLN)
  ↓ 1:N
ProductDenoms (fixed/open amount, prepaid/postpaid)
  ↓ 1:N
ProductDenomMeta (key-value metadata)
```

## Architecture: Hexagonal (Ports & Adapters)
```
[Adapter In]  →  [Port In]  →  [Domain/Service]  →  [Port Out]  →  [Adapter Out]
 Controller        Interface      Business Logic       Interface      Repository/API
```
**Rule**: Setelah selesai coding, panggil Neo untuk review hexagonal compliance.

## Key Decisions
- **UUID IDs** (`@UuidGenerator`) — distributed-friendly, non-sequential
- **LocalDateTime** — modern Java Time API
- **Soft Delete** (`deleted` flag) — preserve data for audit
- **Optimistic Locking** (`@Version`)
- **Mock First** — interfaces dulu (MockProviderService → real impl later)

---

## Constraints & Gotchas

### Database
- **DDL Auto**: `update` in dev, `validate` in prod
- **No Flyway/Liquibase**: Schema managed by Hibernate
- **PostgreSQL**: Use `columnDefinition = "uuid"` for UUID columns

### Security
- **Keycloak**: Users have roles in `List<String>`
- **Admin endpoints**: `@PreAuthorize("hasRole('ADMIN')")`
- **CSRF**: Enabled for Thymeleaf forms

### Performance
- **Virtual Threads**: Enabled (Spring Boot 4+)
- **Batch Size**: 25
- **Caffeine Cache**: Use `@Cacheable` liberally

### Java 25
- Lombok `Unsafe.objectFieldOffset` warnings are normal
- Virtual threads: don't block unnecessarily

---

## Important Files
- `ROADMAP.md` — 4-week plan
- `Tasks.md` — task tracking (TODO/BACKLOG/DONE)
- `pom.xml` — dependencies & build
- `application.yml` — config (profiles: default, dev, prod)
- `.env` — local env vars (DB credentials, secrets)
