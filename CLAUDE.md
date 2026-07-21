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

Always use caveman ultra, ponytail ultra

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
- **Balance via wallet module** — balance ops go through `WalletGateway` → `WalletService` **in-process** (same JVM, JPA-backed). `WalletGateway` is the anti-corruption boundary; swap for a remote HTTP impl behind config when the wallet is split out (see `docs/designs/wallet-split-auth.md`). No network call today.
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

## Agent Token-Saving Pattern
**Trigger check WAJIB sebelum EnterPlanMode / mulai kerja multi-file** — jangan judgment call, cek checklist:
- Scope nyentuh **3+ file**, ATAU
- Nambah/ubah **arsitektur** (layer baru, slice baru, ganti pola), ATAU
- User pake kata "plan", "design", "arsitektur", "roadmap" buat kerjaan non-trivial

Cost stack (per harga real Anthropic — Fable 5 = tier TERMAHAL/paling capable, bukan murah):
- **Plan default = Opus in-session** (session ini, `claude --model claude-opus-4-8`). Checklist TRUE → Opus yang draft plan sendiri, TANPA spawn. Opus udah jalan = gratis, hampir selalu cukup buat plan.
- **Plan = Fable HANYA darurat high-leverage**: spawn `Agent` w/ `subagent_type: "Plan"`, `model: "fable"` cuma kalau keputusan arsitektur berat + ambigu + mahal-kalau-salah (skema baru, ganti pola lintas modul). Tier termahal → tombol darurat, BUKAN reflex tiap plan.
- **Eksekusi = Sonnet/Haiku** (win token utama, paling sering dipanggil):
  - `model: "sonnet"` → default coding/logic: nulis fitur, edit multi-langkah, test.
  - `model: "haiku"` → mekanis murni: grep, list, baca file, format, rename tanpa logika (locator/investigator).

Aturan praktis:
```
plan berat + ambigu + mahal-kalau-salah → Fable (jarang, darurat)
plan biasa                              → Opus session ini, no spawn
coding/logic                            → Sonnet
mekanis murni                           → Haiku
```

Kalau semua checklist FALSE (task kecil, 1-2 file, fix/typo/rename) → skip plan, langsung eksekusi.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
