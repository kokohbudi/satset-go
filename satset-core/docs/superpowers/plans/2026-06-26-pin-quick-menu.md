# Pin/Unpin Quick Menu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let each user pin/unpin sidebar menu items into a personal "Menu Cepat" grid on the dashboard, where pins of lost roles silently disappear.

**Architecture:** New Spring Modulith slice `com.satset.quickmenu` (entity + repository + service + REST controller). A menu item == a Keycloak role with attribute `sidebar=1`; the role `name` is the pin identity. The dashboard grid is built by intersecting the user's currently-held menu roles (`userRoles`, already in the HTTP session) with their pins — so a lost role is never rendered. No cleanup job.

**Tech Stack:** Spring Boot 4, Java 25, Hibernate/JPA (PostgreSQL), Thymeleaf, Lombok, Spring Modulith, JUnit 5 + Mockito + AssertJ.

## Global Constraints

- UUID PKs via `@UuidGenerator`, `columnDefinition = "uuid"` — verbatim from existing entities.
- `LocalDateTime` for timestamps; `@CreatedDate` + `@EntityListeners(AuditingEntityListener.class)`.
- DDL auto = `update` (dev) — no migration files; Hibernate creates the table.
- Constructor injection only; `@Slf4j` for logging; never expose `e.getMessage()` to clients.
- Every new module package gets a `package-info.java` with `@ApplicationModule(type = Type.OPEN)` (matches existing slices) so `ModularityTest` passes.
- CSRF on mutating `fetch()` is auto-attached globally by `layouts/base.html` (lines 16–32) — write NO manual CSRF code in JS.
- Current user id = `userDTO.getProviderUserId()` (Keycloak `sub`).
- Menu identity + render rules must match `templates/components/sidebar.html`: an item shows when `attributes['sidebar'] == '1'` and `attributes['url']` is non-blank; children of a composite role are individual items, the parent itself is only a section header.

---

### Task 1: MenuFlattener + MenuItem (pure logic)

Flattens the nested `userRoles` (`List<RoleInfo>`) into an ordered flat list of menu items, applying the exact sidebar visibility rules. Shared by the service guard and the dashboard builder.

**Files:**
- Create: `src/main/java/com/satset/quickmenu/model/MenuItem.java`
- Create: `src/main/java/com/satset/quickmenu/service/MenuFlattener.java`
- Create: `src/main/java/com/satset/quickmenu/package-info.java`
- Test: `src/test/java/com/satset/quickmenu/service/MenuFlattenerTest.java`

**Interfaces:**
- Consumes: `com.satset.shared.dto.RoleInfo` (fields `name`, `attributes: Map<String,String>`, `children: List<RoleInfo>`).
- Produces:
  - `record MenuItem(String name, String url, String icon, String displayName)`
  - `MenuFlattener.flatten(List<RoleInfo> roles) -> List<MenuItem>` (null-safe; returns empty list on null).

- [ ] **Step 1: Write the failing test**

```java
package com.satset.quickmenu.service;

import com.satset.quickmenu.model.MenuItem;
import com.satset.shared.dto.RoleInfo;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MenuFlattenerTest {

    private final MenuFlattener flattener = new MenuFlattener();

    private RoleInfo role(String name, Map<String, String> attrs, List<RoleInfo> children) {
        return RoleInfo.builder()
                .name(name)
                .attributes(attrs)
                .children(children == null ? List.of() : children)
                .build();
    }

    private Map<String, String> menuAttrs(String url, String icon, String displayName) {
        Map<String, String> m = new HashMap<>();
        m.put("sidebar", "1");
        m.put("url", url);
        if (icon != null) m.put("icon", icon);
        if (displayName != null) m.put("display_name", displayName);
        return m;
    }

    @Test
    void flattensStandaloneSidebarRole() {
        var roles = List.of(role("users", menuAttrs("/users", "icon-users", "Users"), null));
        var out = flattener.flatten(roles);
        assertThat(out).singleElement().satisfies(i -> {
            assertThat(i.name()).isEqualTo("users");
            assertThat(i.url()).isEqualTo("/users");
            assertThat(i.icon()).isEqualTo("icon-users");
            assertThat(i.displayName()).isEqualTo("Users");
        });
    }

    @Test
    void flattensChildrenAndSkipsParent() {
        var child = role("view_catalog", menuAttrs("/catalog", null, null), null);
        var parent = role("catalog_section", Map.of("sidebar", "1"), List.of(child));
        var out = flattener.flatten(List.of(parent));
        assertThat(out).extracting(MenuItem::name).containsExactly("view_catalog");
    }

    @Test
    void skipsNonSidebarRoles() {
        var roles = List.of(role("hidden", Map.of("sidebar", "0", "url", "/x"), null));
        assertThat(flattener.flatten(roles)).isEmpty();
    }

    @Test
    void defaultsIconAndDisplayName() {
        var roles = List.of(role("deposit", menuAttrs("/deposit", null, null), null));
        var item = flattener.flatten(roles).get(0);
        assertThat(item.icon()).isEqualTo("icon-document");
        assertThat(item.displayName()).isEqualTo("deposit");
    }

    @Test
    void nullSafe() {
        assertThat(flattener.flatten(null)).isEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=MenuFlattenerTest`
Expected: FAIL — `MenuItem` / `MenuFlattener` do not exist (compile error).

- [ ] **Step 3: Write the implementation**

`MenuItem.java`:
```java
package com.satset.quickmenu.model;

/** A flattened, render-ready sidebar menu item. */
public record MenuItem(String name, String url, String icon, String displayName) {
}
```

`MenuFlattener.java`:
```java
package com.satset.quickmenu.service;

import com.satset.quickmenu.model.MenuItem;
import com.satset.shared.dto.RoleInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flattens nested role info into the ordered list of sidebar menu items,
 * matching the visibility rules in templates/components/sidebar.html:
 * an item is shown when attribute sidebar == "1" and url is non-blank;
 * children of a composite role are items, the parent is only a header.
 */
@Component
public class MenuFlattener {

    public List<MenuItem> flatten(List<RoleInfo> roles) {
        List<MenuItem> out = new ArrayList<>();
        if (roles == null) {
            return out;
        }
        for (RoleInfo role : roles) {
            List<RoleInfo> children = role.getChildren();
            if (children != null && !children.isEmpty()) {
                for (RoleInfo child : children) {
                    if (isMenu(child)) {
                        out.add(toItem(child));
                    }
                }
            } else if (isMenu(role)) {
                out.add(toItem(role));
            }
        }
        return out;
    }

    private boolean isMenu(RoleInfo r) {
        Map<String, String> a = r.getAttributes();
        return a != null
                && "1".equals(a.get("sidebar"))
                && a.get("url") != null
                && !a.get("url").isBlank();
    }

    private MenuItem toItem(RoleInfo r) {
        Map<String, String> a = r.getAttributes();
        String icon = a.get("icon");
        if (icon == null || icon.isBlank()) {
            icon = "icon-document";
        }
        String displayName = a.get("display_name");
        if (displayName == null || displayName.isBlank()) {
            displayName = r.getName();
        }
        return new MenuItem(r.getName(), a.get("url"), icon, displayName);
    }
}
```

`package-info.java`:
```java
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.satset.quickmenu;
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=MenuFlattenerTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/quickmenu/ src/test/java/com/satset/quickmenu/
git commit -m "feat(quickmenu): MenuFlattener + MenuItem"
```

---

### Task 2: PinnedMenu entity + repository

Persistence for per-user pins.

**Files:**
- Create: `src/main/java/com/satset/quickmenu/model/PinnedMenu.java`
- Create: `src/main/java/com/satset/quickmenu/repository/PinnedMenuRepository.java`

**Interfaces:**
- Produces:
  - Entity `PinnedMenu` with setters/getters (Lombok `@Data`): `userId: String`, `roleName: String`, plus generated `id: UUID`, `createdAt`.
  - `PinnedMenuRepository extends JpaRepository<PinnedMenu, UUID>`:
    - `List<PinnedMenu> findByUserId(String userId)`
    - `boolean existsByUserIdAndRoleName(String userId, String roleName)`
    - `long deleteByUserIdAndRoleName(String userId, String roleName)`

- [ ] **Step 1: Write the entity**

`PinnedMenu.java`:
```java
package com.satset.quickmenu.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/** A single dashboard quick-menu pin owned by one user. */
@Entity
@Table(
        name = "pinned_menu",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pinned_user_role", columnNames = {"user_id", "role_name"}),
        indexes = @Index(name = "idx_pinned_user", columnList = "user_id"))
@EntityListeners(AuditingEntityListener.class)
@Data
public class PinnedMenu {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    /** Keycloak sub of the owning user. */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Keycloak role name = menu identity. */
    @Column(name = "role_name", nullable = false)
    private String roleName;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Write the repository**

`PinnedMenuRepository.java`:
```java
package com.satset.quickmenu.repository;

import com.satset.quickmenu.model.PinnedMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Pin persistence. Spring Data provides the implementation. */
@Repository
public interface PinnedMenuRepository extends JpaRepository<PinnedMenu, UUID> {

    List<PinnedMenu> findByUserId(String userId);

    boolean existsByUserIdAndRoleName(String userId, String roleName);

    long deleteByUserIdAndRoleName(String userId, String roleName);
}
```

- [ ] **Step 3: Verify it compiles**

Run: `mvn -q compile`
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/satset/quickmenu/model/PinnedMenu.java src/main/java/com/satset/quickmenu/repository/PinnedMenuRepository.java
git commit -m "feat(quickmenu): PinnedMenu entity + repository"
```

---

### Task 3: QuickMenuService

Toggle (pin/unpin), pinned-name lookup, and dashboard grid builder.

**Files:**
- Create: `src/main/java/com/satset/quickmenu/service/QuickMenuService.java`
- Test: `src/test/java/com/satset/quickmenu/service/QuickMenuServiceTest.java`

**Interfaces:**
- Consumes: `PinnedMenuRepository` (Task 2), `MenuFlattener` + `MenuItem` (Task 1), `RoleInfo`.
- Produces:
  - `boolean toggle(String userId, String roleName)` — returns `true` if now pinned, `false` if now unpinned. No guard: a junk pin is never rendered by `quickMenu`.
  - `Set<String> pinnedRoleNames(String userId)`.
  - `List<MenuItem> quickMenu(String userId, List<RoleInfo> userRoles)` — pinned ∩ accessible, in sidebar order.

- [ ] **Step 1: Write the failing test**

```java
package com.satset.quickmenu.service;

import com.satset.quickmenu.model.MenuItem;
import com.satset.quickmenu.model.PinnedMenu;
import com.satset.quickmenu.repository.PinnedMenuRepository;
import com.satset.shared.dto.RoleInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuickMenuServiceTest {

    @Mock
    private PinnedMenuRepository repo;

    private QuickMenuService service;
    private List<RoleInfo> userRoles;

    @BeforeEach
    void setup() {
        service = new QuickMenuService(repo, new MenuFlattener());
        userRoles = List.of(RoleInfo.builder()
                .name("users")
                .attributes(Map.of("sidebar", "1", "url", "/users"))
                .children(List.of())
                .build());
    }

    private PinnedMenu pin(String roleName) {
        PinnedMenu p = new PinnedMenu();
        p.setRoleName(roleName);
        return p;
    }

    @Test
    void toggle_pinsWhenNotPinned() {
        when(repo.existsByUserIdAndRoleName("u", "users")).thenReturn(false);

        boolean result = service.toggle("u", "users");

        assertThat(result).isTrue();
        ArgumentCaptor<PinnedMenu> cap = ArgumentCaptor.forClass(PinnedMenu.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getRoleName()).isEqualTo("users");
        assertThat(cap.getValue().getUserId()).isEqualTo("u");
    }

    @Test
    void toggle_unpinsWhenPinned() {
        when(repo.existsByUserIdAndRoleName("u", "users")).thenReturn(true);

        boolean result = service.toggle("u", "users");

        assertThat(result).isFalse();
        verify(repo).deleteByUserIdAndRoleName("u", "users");
        verify(repo, never()).save(any());
    }

    @Test
    void quickMenu_excludesLostRole() {
        when(repo.findByUserId("u"))
                .thenReturn(List.of(pin("users"), pin("ghost")));

        var out = service.quickMenu("u", userRoles);

        assertThat(out).extracting(MenuItem::name).containsExactly("users");
    }

    @Test
    void pinnedRoleNames_returnsNames() {
        when(repo.findByUserId("u")).thenReturn(List.of(pin("users")));

        assertThat(service.pinnedRoleNames("u")).containsExactly("users");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=QuickMenuServiceTest`
Expected: FAIL — `QuickMenuService` does not exist (compile error).

- [ ] **Step 3: Write the implementation**

`QuickMenuService.java`:
```java
package com.satset.quickmenu.service;

import com.satset.quickmenu.model.MenuItem;
import com.satset.quickmenu.model.PinnedMenu;
import com.satset.quickmenu.repository.PinnedMenuRepository;
import com.satset.shared.dto.RoleInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Per-user dashboard quick-menu pin operations. */
@Service
public class QuickMenuService {

    private final PinnedMenuRepository repo;
    private final MenuFlattener flattener;

    public QuickMenuService(PinnedMenuRepository repo, MenuFlattener flattener) {
        this.repo = repo;
        this.flattener = flattener;
    }

    /**
     * Pin the role if not pinned, otherwise unpin it.
     * No access guard: an inaccessible pin simply never renders because
     * {@link #quickMenu} only returns pins present in the user's current menu.
     *
     * @return true if the role is now pinned, false if now unpinned
     */
    @Transactional
    public boolean toggle(String userId, String roleName) {
        if (repo.existsByUserIdAndRoleName(userId, roleName)) {
            repo.deleteByUserIdAndRoleName(userId, roleName);
            return false;
        }
        PinnedMenu pin = new PinnedMenu();
        pin.setUserId(userId);
        pin.setRoleName(roleName);
        repo.save(pin);
        return true;
    }

    public Set<String> pinnedRoleNames(String userId) {
        return repo.findByUserId(userId).stream()
                .map(PinnedMenu::getRoleName)
                .collect(Collectors.toSet());
    }

    /** Pinned items still accessible to the user, in sidebar order. */
    public List<MenuItem> quickMenu(String userId, List<RoleInfo> userRoles) {
        Set<String> pinned = pinnedRoleNames(userId);
        return flattener.flatten(userRoles).stream()
                .filter(item -> pinned.contains(item.name()))
                .toList();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=QuickMenuServiceTest`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/quickmenu/service/QuickMenuService.java src/test/java/com/satset/quickmenu/service/QuickMenuServiceTest.java
git commit -m "feat(quickmenu): QuickMenuService toggle + quickMenu builder"
```

---

### Task 4: QuickMenuController

REST endpoint backing the sidebar star toggle.

**Files:**
- Create: `src/main/java/com/satset/quickmenu/web/QuickMenuController.java`
- Test: `src/test/java/com/satset/quickmenu/web/QuickMenuControllerTest.java`

**Interfaces:**
- Consumes: `QuickMenuService` (Task 3), `com.satset.shared.dto.UserDTO` (request-scoped bean, `getProviderUserId()`).
- Produces: `POST /quick-menu/toggle`, JSON body `{"roleName":"..."}` → `200 {"pinned":bool}`; `400` on blank roleName. Nested records `ToggleRequest(String roleName)`, `ToggleResponse(boolean pinned)`.

- [ ] **Step 1: Write the failing test**

```java
package com.satset.quickmenu.web;

import com.satset.quickmenu.service.QuickMenuService;
import com.satset.shared.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuickMenuControllerTest {

    @Mock
    private QuickMenuService service;

    private QuickMenuController controller;

    @BeforeEach
    void setup() {
        UserDTO userDTO = new UserDTO();
        userDTO.setProviderUserId("u");
        controller = new QuickMenuController(service, userDTO);
    }

    @Test
    void toggle_returnsPinnedTrue() {
        when(service.toggle("u", "users")).thenReturn(true);

        var resp = controller.toggle(new QuickMenuController.ToggleRequest("users"));

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().pinned()).isTrue();
    }

    @Test
    void toggle_blankRoleNameIsBadRequest() {
        var resp = controller.toggle(new QuickMenuController.ToggleRequest("  "));
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=QuickMenuControllerTest`
Expected: FAIL — `QuickMenuController` does not exist (compile error).

- [ ] **Step 3: Write the implementation**

`QuickMenuController.java`:
```java
package com.satset.quickmenu.web;

import com.satset.quickmenu.service.QuickMenuService;
import com.satset.shared.dto.UserDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Toggles a user's dashboard quick-menu pin. CSRF handled globally (layouts/base.html). */
@RestController
@RequestMapping("/quick-menu")
public class QuickMenuController {

    private final QuickMenuService service;
    private final UserDTO userDTO;

    public QuickMenuController(QuickMenuService service, UserDTO userDTO) {
        this.service = service;
        this.userDTO = userDTO;
    }

    @PostMapping("/toggle")
    public ResponseEntity<ToggleResponse> toggle(@RequestBody ToggleRequest req) {
        String roleName = req.roleName();
        if (roleName == null || roleName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean pinned = service.toggle(userDTO.getProviderUserId(), roleName);
        return ResponseEntity.ok(new ToggleResponse(pinned));
    }

    public record ToggleRequest(String roleName) {
    }

    public record ToggleResponse(boolean pinned) {
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=QuickMenuControllerTest`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/satset/quickmenu/web/ src/test/java/com/satset/quickmenu/web/
git commit -m "feat(quickmenu): POST /quick-menu/toggle endpoint"
```

---

### Task 5: Wire model attributes (pinnedRoleNames + quickMenu)

Expose `pinnedRoleNames` to every page (for sidebar star state) and `quickMenu` to the dashboard.

**Files:**
- Modify: `src/main/java/com/satset/UserSessionControllerAdvice.java`
- Modify: `src/main/java/com/satset/shared/web/DashboardController.java`

**Interfaces:**
- Consumes: `QuickMenuService.pinnedRoleNames(userId)`, `QuickMenuService.quickMenu(userId, userRoles)`.
- Produces: model attribute `pinnedRoleNames` (`Set<String>`) on all controller views; `quickMenu` (`List<MenuItem>`) on the dashboard view.

- [ ] **Step 1: Update UserSessionControllerAdvice**

Replace the whole class body of `addAttributes` and add the `QuickMenuService` dependency. Final file:

```java
package com.satset;

import com.satset.identity.client.KeycloakIdentityPort;
import com.satset.quickmenu.service.QuickMenuService;
import com.satset.shared.dto.RoleInfo;
import com.satset.shared.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Controller advice for adding user session attributes to all controllers.
 * Uses shared DTOs (RoleInfo) instead of domain models to avoid coupling
 * shared layer to identity.domain.model package.
 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class UserSessionControllerAdvice {
    private final UserDTO userDTO;
    private final KeycloakIdentityPort keycloakIdentityPort;
    private final QuickMenuService quickMenuService;

    public UserSessionControllerAdvice(UserDTO userDTO,
            KeycloakIdentityPort keycloakIdentityPort,
            QuickMenuService quickMenuService) {
        this.userDTO = userDTO;
        this.keycloakIdentityPort = keycloakIdentityPort;
        this.quickMenuService = quickMenuService;
    }

    @ModelAttribute
    public void addAttributes(Model model, jakarta.servlet.http.HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("user", this.userDTO);
        model.addAttribute("currentPath", request.getRequestURI());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<RoleInfo> roles = (List<RoleInfo>) session.getAttribute("userRoles");
        if (roles == null) {
            String userId = extractUserId(auth);
            if (userId != null) {
                try {
                    roles = keycloakIdentityPort.getMenuRoleInfos(userId);
                    session.setAttribute("userRoles", roles);
                } catch (Exception e) {
                    log.error("Failed to fetch user roles for sidebar", e);
                }
            }
        }
        if (roles != null) {
            model.addAttribute("userRoles", roles);
        }

        String providerUserId = userDTO.getProviderUserId();
        if (providerUserId != null) {
            model.addAttribute("pinnedRoleNames", quickMenuService.pinnedRoleNames(providerUserId));
        }
    }

    private String extractUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        } else if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("sub");
        }
        return null;
    }
}
```

- [ ] **Step 2: Update DashboardController**

Add the `QuickMenuService` dependency, read `userRoles` from session, set `quickMenu`. Apply these edits to `DashboardController.java`:

Constructor + fields — replace the existing field block and constructor:
```java
    private final WalletGateway walletGateway;
    private final UserDTO userDTO;
    private final StoreRepository storeRepository;
    private final QuickMenuService quickMenuService;

    public DashboardController(WalletGateway walletGateway, UserDTO userDTO,
            StoreRepository storeRepository, QuickMenuService quickMenuService) {
        this.walletGateway = walletGateway;
        this.userDTO = userDTO;
        this.storeRepository = storeRepository;
        this.quickMenuService = quickMenuService;
    }
```

Add imports near the top:
```java
import com.satset.quickmenu.service.QuickMenuService;
import com.satset.shared.dto.RoleInfo;
import jakarta.servlet.http.HttpSession;
import java.util.List;
```

Replace the `dashboard` method signature + body:
```java
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        log.info("Accessing dashboard");

        // Set page info for sidebar and header
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("breadcrumb", "Dashboard");
        model.addAttribute("totalBalance", formatBalance(userDTO.getWalletId()));
        model.addAttribute("totalResellers", NumberFormat.getInstance(ID).format(storeRepository.count()));

        @SuppressWarnings("unchecked")
        List<RoleInfo> userRoles = (List<RoleInfo>) session.getAttribute("userRoles");
        model.addAttribute("quickMenu",
                quickMenuService.quickMenu(userDTO.getProviderUserId(), userRoles));

        return "pages/dashboard/index";
    }
```

- [ ] **Step 3: Verify it compiles + existing tests pass**

Run: `mvn -q compile && mvn test -Dtest=ModularityTest`
Expected: BUILD SUCCESS; `ModularityTest` PASS (new `quickmenu` module respects boundaries; only depends on `shared`).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/satset/UserSessionControllerAdvice.java src/main/java/com/satset/shared/web/DashboardController.java
git commit -m "feat(quickmenu): expose pinnedRoleNames + quickMenu to views"
```

---

### Task 6: Sidebar star toggle (UI)

Add a hover-revealed pin star to each sidebar menu item.

**Files:**
- Modify: `src/main/resources/templates/components/sidebar.html`

**Interfaces:**
- Consumes: model attr `pinnedRoleNames` (`Set<String>`), `POST /quick-menu/toggle`.
- Produces: per-item star button calling `toggleQuickMenu(this)`.

- [ ] **Step 1: Wrap the child menu link (currently lines 64-75) with a star**

Replace the child `<th:block th:each="child ...">` inner `<a>` block so the link sits in a `relative group` wrapper next to a star button. New child block:
```html
                        <!-- Children: muncul berdasarkan sidebar flag masing-masing -->
                        <th:block th:each="child : ${role.children}">
                            <div class="relative group"
                                th:if="${child.attributes != null and child.attributes['sidebar'] == '1' and child.attributes['url'] != null and !child.attributes['url'].isEmpty()}">
                                <a th:href="${child.attributes['url']}"
                                    th:with="iconName=${child.attributes['icon'] != null and !child.attributes['icon'].isEmpty() ? child.attributes['icon'] : 'icon-document'}"
                                    class="flex items-center gap-3 px-4 py-3 pr-10 rounded-xl transition-all"
                                    th:classappend="${currentPath != null and currentPath.startsWith(child.attributes['url'])} ? 'bg-primary text-primary-content shadow-lg' : 'hover:bg-primary/10 hover:text-primary'">
                                    <svg th:replace="~{components/icons :: __${iconName}__(class='w-5 h-5')}"></svg>
                                    <span
                                        th:text="${child.attributes['display_name'] != null and !child.attributes['display_name'].isEmpty()} ? ${child.attributes['display_name']} : ${child.name}">Menu
                                        Item</span>
                                </a>
                                <button type="button" th:data-role="${child.name}" onclick="toggleQuickMenu(this)"
                                    title="Pin ke menu cepat"
                                    class="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity"
                                    th:classappend="${pinnedRoleNames != null and pinnedRoleNames.contains(child.name)} ? 'text-primary' : 'text-base-content/30'">
                                    <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
                                        <path d="M9.05 2.93c.3-.92 1.6-.92 1.9 0l1.42 4.36a1 1 0 00.95.69h4.59c.97 0 1.37 1.24.59 1.81l-3.71 2.7a1 1 0 00-.36 1.12l1.41 4.36c.3.92-.75 1.69-1.54 1.12l-3.71-2.7a1 1 0 00-1.18 0l-3.71 2.7c-.79.57-1.84-.2-1.54-1.12l1.41-4.36a1 1 0 00-.36-1.12l-3.71-2.7c-.78-.57-.38-1.81.59-1.81h4.59a1 1 0 00.95-.69l1.42-4.36z"/>
                                    </svg>
                                </button>
                            </div>
                        </th:block>
```

- [ ] **Step 2: Wrap the standalone menu link (currently lines 79-88) with a star**

Replace the standalone `<a th:if="${role.attributes ... role.children empty}">` block:
```html
                    <!-- CASE 2: Role tanpa children tapi punya sidebar=1 dan url (sebagai standalone menu item) -->
                    <div class="relative group"
                        th:if="${role.attributes != null and role.attributes['sidebar'] == '1' and role.attributes['url'] != null and !role.attributes['url'].isEmpty() and (role.children == null or role.children.isEmpty())}">
                        <a th:href="${role.attributes['url']}"
                            th:with="iconName=${role.attributes['icon'] != null and !role.attributes['icon'].isEmpty() ? role.attributes['icon'] : 'icon-document'}"
                            class="flex items-center gap-3 px-4 py-3 pr-10 rounded-xl transition-all"
                            th:classappend="${currentPath != null and currentPath.startsWith(role.attributes['url'])} ? 'bg-primary text-primary-content shadow-lg' : 'hover:bg-primary/10 hover:text-primary'">
                            <svg th:replace="~{components/icons :: __${iconName}__(class='w-5 h-5')}"></svg>
                            <span
                                th:text="${role.attributes['display_name'] != null and !role.attributes['display_name'].isEmpty()} ? ${role.attributes['display_name']} : ${role.name}">Menu
                                Item</span>
                        </a>
                        <button type="button" th:data-role="${role.name}" onclick="toggleQuickMenu(this)"
                            title="Pin ke menu cepat"
                            class="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded-lg opacity-0 group-hover:opacity-100 transition-opacity"
                            th:classappend="${pinnedRoleNames != null and pinnedRoleNames.contains(role.name)} ? 'text-primary' : 'text-base-content/30'">
                            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20" aria-hidden="true">
                                <path d="M9.05 2.93c.3-.92 1.6-.92 1.9 0l1.42 4.36a1 1 0 00.95.69h4.59c.97 0 1.37 1.24.59 1.81l-3.71 2.7a1 1 0 00-.36 1.12l1.41 4.36c.3.92-.75 1.69-1.54 1.12l-3.71-2.7a1 1 0 00-1.18 0l-3.71 2.7c-.79.57-1.84-.2-1.54-1.12l1.41-4.36a1 1 0 00-.36-1.12l-3.71-2.7c-.78-.57-.38-1.81.59-1.81h4.59a1 1 0 00.95-.69l1.42-4.36z"/>
                            </svg>
                        </button>
                    </div>
```

- [ ] **Step 3: Add the toggle script at the end of the fragment**

Immediately before the closing `</aside>`/fragment-root close tag of `sidebar.html`, add:
```html
        <script>
            function toggleQuickMenu(btn) {
                const roleName = btn.dataset.role;
                fetch('/quick-menu/toggle', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ roleName })
                })
                    .then(r => r.ok ? r.json() : null)
                    .then(d => {
                        if (!d) return;
                        btn.classList.toggle('text-primary', d.pinned);
                        btn.classList.toggle('text-base-content/30', !d.pinned);
                    })
                    .catch(() => {});
            }
        </script>
```

- [ ] **Step 4: Verify rendering in the running app**

Run: `mvn spring-boot:run` (or the project's run skill), log in, hover a sidebar item → star appears; click → star turns primary; reload → star stays primary (persisted). Click again → reverts.
Expected: star toggles and persists; no console errors; network `POST /quick-menu/toggle` returns `{"pinned":true|false}`.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/components/sidebar.html
git commit -m "feat(quickmenu): hover pin star on sidebar items"
```

---

### Task 7: Dashboard "Menu Cepat" grid (replace Aksi Cepat)

Replace the hardcoded Aksi Cepat card with the pinned grid; render nothing when empty.

**Files:**
- Modify: `src/main/resources/templates/pages/dashboard/index.html` (the Quick Actions card, currently lines 96-135)

**Interfaces:**
- Consumes: model attr `quickMenu` (`List<MenuItem>`), `components/icons` fragments.

- [ ] **Step 1: Replace the Quick Actions card**

Replace the entire `<!-- Quick Actions -->` card block (the `<div class="card ... lg:col-span-2">` … `</div>` ending at line 135) with:
```html
        <!-- Quick Menu (user-pinned). Hidden entirely when nothing is pinned. -->
        <div th:if="${quickMenu != null and !quickMenu.isEmpty()}"
            class="card bg-base-100 shadow-lg border border-base-300/50 lg:col-span-2">
            <div class="card-body">
                <h2 class="card-title text-lg font-bold mb-4">Menu Cepat</h2>
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                    <a th:each="item : ${quickMenu}" th:href="${item.url}"
                        class="flex flex-col items-center justify-center gap-2 py-4 min-h-[82px] rounded-xl border border-base-300 text-base-content/70 hover:border-primary hover:text-primary hover:bg-primary/10 transition-all duration-200">
                        <svg th:replace="~{components/icons :: __${item.icon}__(class='w-6 h-6')}"></svg>
                        <span class="text-xs font-semibold" th:text="${item.displayName}">Menu</span>
                    </a>
                </div>
            </div>
        </div>
```

- [ ] **Step 2: Verify in the running app**

Run: with the app running and logged in: with no pins, the dashboard shows no "Menu Cepat" card (only Status Sistem). Pin an item from the sidebar, reload `/dashboard` → "Menu Cepat" card appears with that item. Remove the user's role for that menu (Keycloak) and re-login → item gone from the grid.
Expected: card visibility + lost-role disappearance behave as described.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/pages/dashboard/index.html
git commit -m "feat(quickmenu): dashboard Menu Cepat grid replaces Aksi Cepat"
```

---

### Task 8: Full verification

- [ ] **Step 1: Run the whole suite**

Run: `mvn test`
Expected: BUILD SUCCESS, no regressions (includes `ModularityTest`, the 3 new test classes, and all existing tests).

- [ ] **Step 2: Final commit (if any lockfile/formatting changes)**

```bash
git status
# commit only if there are stray changes; otherwise nothing to do
```

---

## Notes / Deliberate Cuts (ponytail)

- No custom pin order — items render in sidebar/menu order. Add a `sort_order` column + reorder endpoint when a user actually asks to reorder.
- No server-side access guard on pin — `quickMenu()` only renders pins present in the user's current menu, so a junk/inaccessible pin is silently invisible (same mechanism as a lost role). No `BusinessException`/403 path to maintain.
- No live dashboard sync — the star updates the DB and its own icon; the dashboard grid refreshes on next load.
- No orphan-row cleanup — `quickMenu()` filters lost roles at render; stale rows are invisible and harmless.
- When `quickMenu` is empty the dashboard's 3-column grid leaves "Status Sistem" alone in one column. Acceptable; revisit layout only if it looks off.
- No `@DataJpaTest` for the repository — the three methods are Spring Data derived queries (framework-generated); the real logic (flatten + intersect) is covered by `MenuFlattenerTest` + `QuickMenuServiceTest`.
