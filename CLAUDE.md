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

## Architecture: Layered (vertical slice per feature)
```
<feature>/
  web/         Controllers (REST + Thymeleaf)
  service/     Business logic
  repository/  Spring Data JpaRepository  (entity == domain model)
  model/       @Entity classes, enums, value records
  dto/         request/response DTOs
  client/      external-system clients + their ports (Keycloak, wallet, provider)
```
Flow: `Controller → Service → Repository`. Interfaces/ports ONLY at external or
polymorphic boundaries (Keycloak, wallet HTTP, provider) — NOT for own-DB access
and NOT one-impl use-case interfaces. (Migrated from Hexagonal, 2026-06.)

## Key Decisions
- **UUID IDs** (`@UuidGenerator`) — distributed-friendly, non-sequential
- **LocalDateTime** — modern Java Time API
- **Soft Delete** (`deleted` flag) — preserve data for audit
- **Optimistic Locking** (`@Version`)
- **Entity == domain model** — one `@Entity` class, no separate domain/entity split or mappers
- **Balance is remote-only** — all balance ops go to the wallet service over HTTP (`WalletClientAdapter`)
- **Mock First** — for external boundaries (e.g. `MockProviderAdapter` → real impl later)

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
