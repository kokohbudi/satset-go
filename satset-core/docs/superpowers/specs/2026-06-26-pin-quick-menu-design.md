# Pin/Unpin Menu → Quick Menu (per user)

**Date:** 2026-06-26
**Status:** Approved, ready for implementation plan

## Problem

Each user wants a personal "quick menu" on the dashboard: pin/unpin sidebar menu
items via a toggle that appears on hover. Pins are per-user. If a user loses the
role that grants a menu, that pinned item must disappear from the quick menu too.

## Core Mechanism

A menu item == a Keycloak role with attribute `sidebar=1`. The role `name` is the
stable identity. We store pins as `(user_id, role_name)`.

The dashboard quick-grid is built by iterating **only** the user's currently-held
menu roles (`userRoles`, already loaded into the HTTP session by
`UserSessionControllerAdvice`) and keeping those whose name is pinned. Because the
iteration source is the roles the user *currently holds*, a lost role is never
iterated, so its pin silently vanishes. No cleanup job; the orphan DB row is
harmless and ignored at render time.

## Components

### 1. Persistence — `PinnedMenu` entity (satset-core DB)

Table `pinned_menu`:

| column      | type         | notes                          |
|-------------|--------------|--------------------------------|
| id          | uuid         | `@UuidGenerator` PK            |
| user_id     | varchar      | Keycloak `sub` (provider id)   |
| role_name   | varchar      | KC role name = menu identity   |
| sort_order  | int          | append order (max existing)    |
| created_at  | timestamp    | LocalDateTime                  |

- Unique constraint `(user_id, role_name)`.
- Index on `user_id`.

`PinnedMenuRepository extends JpaRepository<PinnedMenu, UUID>`:
- `List<PinnedMenu> findByUserIdOrderBySortOrderAsc(String userId)`
- `boolean existsByUserIdAndRoleName(String userId, String roleName)`
- `long deleteByUserIdAndRoleName(String userId, String roleName)`
- `long countByUserId(String userId)`

### 2. Flatten helper

`userRoles` is a nested `List<RoleInfo>` (parents with `children`). A helper
flattens it into an ordered list of menu items — `{name, url, icon, display_name}`
— taking only entries with `attributes['sidebar'] == '1'` and a non-empty `url`
(both standalone roles and children, matching the existing sidebar render logic in
`templates/components/sidebar.html`). Shared by the service guard and the dashboard
builder so the rule lives in one place, not duplicated in Thymeleaf.

### 3. Service — `PinnedMenuService`

- `boolean toggle(String userId, String roleName, List<RoleInfo> userRoles)`
  - If `existsByUserIdAndRoleName` → delete, return `false` (unpinned).
  - Else: **guard** — `roleName` must be in the flattened accessible menu names of
    `userRoles`; reject otherwise (prevents pinning arbitrary/inaccessible roles).
    Insert with `sort_order = countByUserId`. Return `true` (pinned).
- `Set<String> pinnedRoleNames(String userId)` — for sidebar star state.
- `List<MenuItem> quickMenu(String userId, List<RoleInfo> userRoles)` — flatten ∩
  pinned, ordered by `sort_order`. Used by the dashboard.

Current user id = `userDTO.getProviderUserId()` (KC sub).

### 4. Endpoint — `PinnedMenuController`

- `POST /quick-menu/toggle`, body `{ "roleName": "..." }` → `{ "pinned": true|false }`.
- Reads `userRoles` from session for the guard.
- CSRF: send the token via the same fetch-mutation convention used since the CSRF
  hardening commit (verify exact header/meta during implementation).

### 5. Render

- `UserSessionControllerAdvice`: add `pinnedRoleNames` (Set) to the model each
  request (one small indexed query). Drives sidebar star fill state.
- `templates/components/sidebar.html`: each menu `<a>` gets a hover-revealed star
  button; filled when `role.name ∈ pinnedRoleNames`, outline otherwise. Click →
  `fetch('/quick-menu/toggle')` → flip the star icon in place.
- `DashboardController`: add `quickMenu` model attribute via
  `PinnedMenuService.quickMenu(...)`.
- `templates/pages/dashboard/index.html`: replace the hardcoded "Aksi Cepat" card
  with a grid driven by `quickMenu` (icon + display_name + url). If `quickMenu` is
  empty → render no card at all.

## Scope Cuts (deliberate)

- **No drag-reorder.** `sort_order` is append-only. Add when asked.
- **No live dashboard sync.** Star toggle updates DB + its own icon; the dashboard
  quick-grid refreshes on next dashboard load.
- **No orphan-row cleanup.** Filter-at-render makes stale pins invisible; cleaning
  them is unnecessary work.

## Testing

- Service: guard rejects a role not in the accessible set; toggle is idempotent
  (pin → unpin → pin); unpin removes the row.
- Flatten + filter: a pin whose role is absent from `userRoles` is excluded from
  `quickMenu` (the "lost role" case).
- Repository: `findByUserIdOrderBySortOrderAsc` returns insertion order.
