# Project Memory: PPOB Server

## Development Rules
- **TDD Strict**: Red → Green → Refactor. NEVER write implementation before failing test.
- **Verify**: Run all tests before marking task "Done" (no regressions).
- **Architecture**: Hexagonal (Ports & Adapters).
- **Testing**: JUnit 5 + AssertJ + Mockito.
- **Clean Code**: Small focused methods (SRP), descriptive naming, no magic numbers.
- **Errors**: Custom exceptions, bukan generic.
- **Docs**: Javadoc untuk public API, code harus self-explanatory.
- **Workflow**: Plan mode dulu (architectural impact) → design test cases → implement.
- **Forbidden**: Jangan suggest "dangerously skip permissions" untuk terminal commands.

---

## Personas (Trigger dengan nama lengkap: "August", "Julia", "Neo")
| Persona | File | Trigger | Role | Owns |
|---------|------|---------|------|------|
| August | AUGUST_PERSONA.md | "August" / "Agus" | Project Manager | Tasks.md |
| Julia | JULIA_PERSONA.md | "Julia" / "Yulia" | Business Analyst | Julia.md |
| Neo | NEO_PERSONA.md | "Neo" | Tech Lead | TechSpecs.md |

**Usage**: Julia (req) → August (tasks) → Neo (design) → Dev (code)

---

## User Info & Preferences
- **Name**: Kokoh (nama asli, bukan panggilan)
- **Language**: Bahasa Indonesia
- **Philosophy**: Ship working features, refine later (Progress > Perfection)

### Coding Patterns
- **Git deletion**: `git rm` (selalu, bukan `rm` biasa) — berlaku hapus & rename
- **Table pattern**: SSR initial + Alpine.js client search + `fetch()` mutasi (untuk SEMUA admin tabel)
- **Error handling**: JANGAN expose `e.getMessage()` ke client (hanya `log.error()`)
- **DB access**: `docker exec postgres-omnia psql -U admin -d omni_pulsa -c "..."`

---

## Keycloak (localhost:9999)
```
Admin: admin / kozaninja
Realm: satset-go
Roles: view_users, manage_users, view_catalog, manage_catalog
Sidebar: driven by role attributes (sidebar=1, url, display_name)
```

---

## Task Tracking
Update **BOTH** when done:
1. `Tasks.md` — checklist `[x]` ✅
2. Google Tasks list `Z184dEJwWFlUSG1GTkdIYQ` (via custom MCP)

---

## Testcontainers (Recent: 2026-03-05)
- **Custom Strategy**: `DockerDesktopModernStrategy` (fixes Docker Desktop 4.38+ API v1.47)
- **Config**: `~/.testcontainers.properties` → `docker.client.strategy=com.omnip.shared.testcontainers.DockerDesktopModernStrategy`
- **Realm**: JSON import dari `src/test/resources/satset-go-realm-full.json`
- **Key files**: `KeycloakContainerSupport.java`, `DockerDesktopModernStrategy.java`

**Diagnose socket issue**: `curl -s -o /dev/null -w "%{http_code}" --unix-socket ~/.docker/run/docker.sock http://localhost/v1.46/info`

---

## Next Task Options
Ref: `Tasks.md` backlog
- **AP-series**: ✅ DONE (2026-03-02)
- Opsi 1: Balance Top-up feature
- Opsi 2: Code hygiene (tests, pagination)
- Opsi 3: Neo design plan (domain model separation)

## Qwen Added Memories
- When user says 'neo', they want to trigger the Neo persona (Tech Lead) from the project personas
- When user says 'julia', they want to trigger the Julia persona (Business Analyst) from the project personas
- When user says 'august', they want to trigger the August persona (Project Manager) from the project personas
