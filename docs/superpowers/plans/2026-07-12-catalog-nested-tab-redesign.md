# Catalog Nested-Tab Redesign + Denom Reassign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the three full-page catalog admin screens with one single-page nested-tab UI (Category → Product → Denom, client-side, no reload), and add the ability to reassign an existing denom to a different product.

**Architecture:** Backend adds one nullable `productId` field to `UpdateDenomRequest`; `DenomDomainService.update` moves the denom when that field names a different, existing product (category follows the product). Frontend collapses `categories.html` + `products.html` + `denoms.html` into one `index.html` driven by a single Alpine component `catalogManager()` holding `activeCategory`/`activeProduct` state; existing table/modal/sync markup is carried over, not rewritten. Denoms are lazy-fetched per product via the existing REST endpoint.

**Tech Stack:** Spring Boot 4.0.1, Java 25, Thymeleaf + Alpine.js + DaisyUI/Tailwind, JUnit 5 + AssertJ + Mockito, PostgreSQL/Hibernate.

## Global Constraints

- Java 25, Spring Boot 4.0.1, Maven. Module: `satset-core`, slice: `catalog`.
- TDD strict: Red → Green → Refactor. No implementation before a failing test (backend).
- Entity == domain model; raw UUID FKs (`ProductDenoms.productId` NOT NULL, `Products.categoryId`).
- Never expose `e.getMessage()` to the client — `log.error()` only; toasts show generic/DTO error text.
- Deletions/renames use `git rm` (never plain `rm`).
- SSR-first: seed initial page data as inline JS globals (`/*[[${initialX}]]*/`); client `fetch()` only for mutations/lazy loads, never the first paint.
- Testcontainers Keycloak image: `quay.io/keycloak/keycloak:26.2.1` (never `:latest`).
- Run tests from repo root: `./mvnw -pl satset-core test`.

## Non-Goals

- No denom-first sync. DF sync stays product-first (`CatalogSyncService.reconcileForProduct(productId)`), unchanged.
- No orphan/staging denoms — `ProductDenoms.productId` stays NOT NULL.
- No change to category/product edit logic — reuse existing endpoints + modals.

## File Structure

**Backend (Phase A):**
- Modify: `satset-core/src/main/java/com/satset/catalog/dto/UpdateDenomRequest.java` — add `UUID productId`.
- Modify: `satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java:113-136` — reassign logic in `update`.
- Modify: `satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java` — 3 existing callsites + 2 new tests.
- No change: `AdminCatalogController.updateDenom` (already forwards `req` to service).

**Frontend (Phase B):**
- Create: `satset-core/src/main/resources/templates/pages/admin/catalog/index.html` — single nested-tab page.
- Modify: `satset-core/src/main/java/com/satset/catalog/web/AdminCatalogPageController.java` — serve `index`, seed categories+products+types.
- Modify: `satset-core/src/test/java/com/satset/catalog/web/AdminCatalogPageControllerTest.java` — assert new page + model.
- Remove (`git rm`): `templates/pages/admin/catalog/{categories,products,denoms}.html` + their old page routes.
- Modify: any template linking to `/admin/catalog/categories|products|.../denoms` (sidebar/breadcrumbs) → point to `/admin/catalog`.

---

# Phase A — Backend: Denom Reassign

### Task A1: Add nullable `productId` to `UpdateDenomRequest` (compile-safe prep)

Adding a record component changes the canonical constructor arity, so the 3 existing test callsites must be updated in the same task to keep the suite compiling and green. The service still ignores the field after this task — behavior change comes in A2.

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/dto/UpdateDenomRequest.java`
- Modify: `satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java:321,350,366`

**Interfaces:**
- Produces: `UpdateDenomRequest.productId()` → `UUID` (nullable; null = keep current product).

- [ ] **Step 1: Add the field**

Append `productId` as the LAST component (nullable, no validation annotation — reassign is optional):

```java
public record UpdateDenomRequest(
    @NotBlank @Size(max = 100) String code,
    @NotBlank @Size(max = 150) String name,
    @NotNull DenomType denomType,
    BigDecimal nominal,
    @NotNull @Positive BigDecimal price,
    BigDecimal basePrice,
    BigDecimal adminFee,
    Integer validityDays,
    Long quotaMb,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    boolean requiresInquiry,
    Integer stockAvailable,
    boolean active,
    int sortOrder,
    java.util.UUID productId
) {}
```

- [ ] **Step 2: Fix the 3 existing test callsites**

Append `, null` as the final arg to each existing `new UpdateDenomRequest(...)`:

- `DenomDomainServiceTest.java:326` — change `false, 100, false, 5);` → `false, 100, false, 5, null);`
- `DenomDomainServiceTest.java:354` — change `false, null, true, 1);` → `false, null, true, 1, null);`
- `DenomDomainServiceTest.java:370` — change `false, null, true, 1);` → `false, null, true, 1, null);`

- [ ] **Step 3: Run existing denom tests — verify still green**

Run: `./mvnw -pl satset-core test -Dtest=DenomDomainServiceTest`
Expected: PASS (field added, service unchanged, callsites compile).

- [ ] **Step 4: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/dto/UpdateDenomRequest.java \
        satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java
git commit -m "refactor(catalog): add nullable productId to UpdateDenomRequest"
```

---

### Task A2: Reassign denom to a different product in `DenomDomainService.update`

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java:113-136`
- Test: `satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java`

**Interfaces:**
- Consumes: `UpdateDenomRequest.productId()`, `ProductRepository.findById(UUID)` (already injected as `productRepository`), `ResourceNotFoundException(String, Object)`.
- Behavior: if `productId` is non-null and differs from the denom's current `productId`, validate the target product exists then move the denom; null or same value = no move.

- [ ] **Step 1: Write the failing reassign test**

Add to `DenomDomainServiceTest` (`existingDenom`, `denomId`, and `productId` already exist as test fixtures; `existingDenom` currently belongs to `productId`):

```java
@Test
void update_ReassignsProduct_WhenDifferentProductIdProvided() throws BusinessException {
    UUID newProductId = UUID.randomUUID();
    Products newProduct = new Products();
    newProduct.setId(newProductId);
    UpdateDenomRequest req = new UpdateDenomRequest(
            "TLKM5", "Telkomsel 5K", DenomType.FIXED_DENOM,
            new BigDecimal("5000"), new BigDecimal("5800"), null, null,
            null, null, null, null,
            false, null, true, 5, newProductId);
    when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
    when(denomRepository.existsByCodeAndIdNot("TLKM5", denomId)).thenReturn(false);
    when(productRepository.findById(newProductId)).thenReturn(Optional.of(newProduct));
    when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

    ProductDenoms result = denomService.update(denomId, req);

    assertEquals(newProductId, result.getProductId());
}
```

- [ ] **Step 2: Run — verify it fails**

Run: `./mvnw -pl satset-core test -Dtest=DenomDomainServiceTest#update_ReassignsProduct_WhenDifferentProductIdProvided`
Expected: FAIL — `result.getProductId()` still equals the original `productId` (service ignores the field).

- [ ] **Step 3: Implement reassign**

In `DenomDomainService.update`, insert BEFORE the final `return denomRepository.save(denom);` (after `denom.setSortOrder(req.sortOrder());` at line 134):

```java
if (req.productId() != null && !req.productId().equals(denom.getProductId())) {
    productRepository.findById(req.productId())
        .orElseThrow(() -> new ResourceNotFoundException("Product", req.productId()));
    denom.setProductId(req.productId());
}
```

- [ ] **Step 4: Run — verify it passes**

Run: `./mvnw -pl satset-core test -Dtest=DenomDomainServiceTest#update_ReassignsProduct_WhenDifferentProductIdProvided`
Expected: PASS.

- [ ] **Step 5: Write the failing "missing target product" test**

```java
@Test
void update_ReassignToMissingProduct_ThrowsResourceNotFound() {
    UUID missingProductId = UUID.randomUUID();
    UpdateDenomRequest req = new UpdateDenomRequest(
            "TLKM5", "Telkomsel 5K", DenomType.FIXED_DENOM,
            null, new BigDecimal("5000"), null, null,
            null, null, null, null,
            false, null, true, 1, missingProductId);
    when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
    when(denomRepository.existsByCodeAndIdNot("TLKM5", denomId)).thenReturn(false);
    when(productRepository.findById(missingProductId)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
            () -> denomService.update(denomId, req));
    verify(denomRepository, never()).save(any());
}
```

- [ ] **Step 6: Run both new tests — verify pass**

Run: `./mvnw -pl satset-core test -Dtest=DenomDomainServiceTest`
Expected: PASS (existing update tests pass `null` productId → no move; both new tests pass).

- [ ] **Step 7: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java \
        satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java
git commit -m "feat(catalog): reassign denom to different product on update"
```

---

# Phase B — Frontend: Single Nested-Tab Page

> No Alpine unit-test harness exists; frontend tasks verify via `AdminCatalogPageControllerTest` (integration, renders page + asserts SSR model) and a manual run checkpoint (Task B5). Keep each task committing a working page.

### Task B1: Serve the new single page from the controller

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/web/AdminCatalogPageController.java`
- Test: `satset-core/src/test/java/com/satset/catalog/web/AdminCatalogPageControllerTest.java`

**Interfaces:**
- Produces: `GET /admin/catalog` → view `pages/admin/catalog/index`, model attributes `initialCategories` (`List<CategoryDTO>`), `initialProducts` (`List<ProductDTO>`), `categoryTypes` (`CategoryType[]`), `denomTypes` (`DenomType[]`).

- [ ] **Step 1: Write the failing controller test**

Add to `AdminCatalogPageControllerTest` (follow the existing MockMvc + `@WithMockUser(roles=...)` style already in that file):

```java
@Test
@WithMockUser(roles = "view_catalog")
void catalogRoot_rendersSinglePage_withSeededData() throws Exception {
    when(manageCategoriesUseCase.findAllForAdmin()).thenReturn(List.of());
    when(manageProductsUseCase.findAllForAdmin()).thenReturn(List.of());

    mockMvc.perform(get("/admin/catalog"))
            .andExpect(status().isOk())
            .andExpect(view().name("pages/admin/catalog/index"))
            .andExpect(model().attributeExists("initialCategories"))
            .andExpect(model().attributeExists("initialProducts"))
            .andExpect(model().attributeExists("categoryTypes"))
            .andExpect(model().attributeExists("denomTypes"));
}
```

- [ ] **Step 2: Run — verify it fails**

Run: `./mvnw -pl satset-core test -Dtest=AdminCatalogPageControllerTest#catalogRoot_rendersSinglePage_withSeededData`
Expected: FAIL — current `catalogRoot()` returns a redirect, view name mismatch.

- [ ] **Step 3: Rewrite `catalogRoot()` to render the page**

Replace the existing `@GetMapping catalogRoot()` (lines 39-42) with:

```java
@GetMapping
public String catalogRoot(Model model) {
    model.addAttribute("currentPage", "admin-catalog");
    model.addAttribute("breadcrumb", "Katalog");
    model.addAttribute("categoryTypes", CategoryType.values());
    model.addAttribute("denomTypes", DenomType.values());

    List<CategoryDTO> categories = manageCategoriesUseCase.findAllForAdmin().stream()
            .map(CatalogDtoMapper::toCategoryDTO).toList();
    model.addAttribute("initialCategories", categories);

    List<ProductDTO> products = manageProductsUseCase.findAllForAdmin().stream()
            .map(CatalogDtoMapper::toProductDTO).toList();
    model.addAttribute("initialProducts", products);

    return "pages/admin/catalog/index";
}
```

Leave the old `/categories`, `/products`, `/products/{id}/denoms` routes in place for now (removed in Task B4). Keep the existing imports; add `import com.satset.catalog.dto.ProductDTO;` if not present.

- [ ] **Step 4: Create a minimal `index.html` so the view resolves**

Create `satset-core/src/main/resources/templates/pages/admin/catalog/index.html` as a stub that decorates the base layout and seeds the globals (full UI comes in Task B2):

```html
<!DOCTYPE html>
<html lang="id" xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security"
      layout:decorate="~{layouts/base}">
<head><title>Katalog</title></head>
<body>
<div layout:fragment="content" x-data="catalogManager()">
    <h1 class="text-2xl font-bold">Katalog</h1>
</div>
<th:block layout:fragment="scripts">
<script th:inline="javascript">
    const INITIAL_CATEGORIES = /*[[${initialCategories}]]*/ [];
    const INITIAL_PRODUCTS   = /*[[${initialProducts}]]*/ [];
    const CATEGORY_TYPES     = /*[[${categoryTypes}]]*/ [];
    const DENOM_TYPES        = /*[[${denomTypes}]]*/ [];
    function catalogManager() { return {}; }
</script>
</th:block>
</body>
</html>
```

- [ ] **Step 5: Run — verify it passes**

Run: `./mvnw -pl satset-core test -Dtest=AdminCatalogPageControllerTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/web/AdminCatalogPageController.java \
        satset-core/src/main/resources/templates/pages/admin/catalog/index.html \
        satset-core/src/test/java/com/satset/catalog/web/AdminCatalogPageControllerTest.java
git commit -m "feat(catalog): serve single-page catalog shell at /admin/catalog"
```

---

### Task B2: Build the nested-tab UI in `index.html`

Carry over table/modal/sync markup from the three old templates into one `catalogManager()`; the page-constant `PRODUCT_ID`/`categoryId`/`productName` become reactive state `activeCategory`/`activeProduct`. This is a UI carry-over, not a rewrite of the behavior.

**Files:**
- Modify: `satset-core/src/main/resources/templates/pages/admin/catalog/index.html`

**Interfaces:**
- Consumes globals seeded in B1. Uses existing REST endpoints unchanged:
  - `GET /api/admin/catalog/products/{productId}/denoms` (lazy denom load)
  - `PUT/POST/DELETE /api/admin/catalog/categories|products|denoms/...` (existing CRUD)
  - `GET /api/admin/catalog/products/{productId}/pricelist-compare`, `POST .../sync/denoms` (existing sync)

- [ ] **Step 1: Replace the `content` fragment with the nested-tab layout**

Structure (DaisyUI `tabs tabs-bordered`, Alpine state-driven like `transactions/index.html:29-31` — click sets state, no navigation):

```html
<div layout:fragment="content" x-data="catalogManager()"
     th:data-can-manage-cat="${#authorization.expression('hasRole(''REALM_manage_catalog'')')}"
     th:data-can-manage-denom="${#authorization.expression('hasRole(''REALM_manage_denoms'')')}">

    <!-- Tab kategori -->
    <div role="tablist" class="tabs tabs-bordered mb-3">
        <a role="tab" class="tab" :class="activeCategory === null ? 'tab-active' : ''"
           @click="selectCategory(null)">Semua</a>
        <template x-for="c in categories" :key="c.id">
            <a role="tab" class="tab" :class="activeCategory === c.id ? 'tab-active' : ''"
               @click="selectCategory(c.id)" x-text="c.name"></a>
        </template>
        <button x-show="canManageCat" class="btn btn-ghost btn-xs ml-auto" @click="openCategoryModal()">+ Kategori</button>
    </div>

    <!-- Subtab produk (hidden when Semua-kategori) -->
    <div role="tablist" class="tabs tabs-bordered mb-4" x-show="activeCategory !== null">
        <a role="tab" class="tab" :class="activeProduct === null ? 'tab-active' : ''"
           @click="selectProduct(null)">Semua</a>
        <template x-for="p in productsInCategory" :key="p.id">
            <a role="tab" class="tab" :class="activeProduct === p.id ? 'tab-active' : ''"
               @click="selectProduct(p.id)" x-text="p.name"></a>
        </template>
        <button x-show="canManageCat" class="btn btn-ghost btn-xs ml-auto" @click="openProductModal()">+ Produk</button>
    </div>

    <!-- Panels -->
    <div x-show="activeCategory === null"><!-- flat products table (carry over from products.html:table) --></div>
    <div x-show="activeCategory !== null && activeProduct === null"><!-- flat denoms in category --></div>
    <div x-show="activeProduct !== null"><!-- denoms of product (carry over from denoms.html table + sync btns) --></div>

    <!-- Modals: category (from categories.html), product (from products.html), denom (from denoms.html) -->
</div>
```

Carry over verbatim into the panels/modals:
- Products table markup from `products.html` (rows, status badges, edit/delete buttons).
- Denoms table + Sync buttons + Sync Preview Modal from `denoms.html:60-276`.
- Category edit modal from `categories.html` (edit form + `saveCategory()`).
- Product edit modal from `products.html:120-160` (incl. the category `<select x-model="form.categoryId">` that moves a product between categories).
- Denom create/edit modal from `denoms.html:124-238` (cascade selector added in Task B3).

- [ ] **Step 2: Write `catalogManager()` state + tab methods**

Replace the stub script with the merged component. State + tab logic (the CRUD/sync methods are carried over from the three old `*Manager()` functions, with `this.productId`→`this.activeProduct` and `this.categoryId`→`this.activeCategory`):

```javascript
function catalogManager() {
    return {
        canManageCat:   document.querySelector('[data-can-manage-cat]')?.dataset.canManageCat === 'true',
        canManageDenom: document.querySelector('[data-can-manage-denom]')?.dataset.canManageDenom === 'true',
        categories: INITIAL_CATEGORIES,
        products:   INITIAL_PRODUCTS,
        categoryTypes: CATEGORY_TYPES,
        denomTypes: DENOM_TYPES,
        activeCategory: null,        // null = Semua
        activeProduct: null,         // null = Semua
        denomsByProduct: {},         // cache: productId -> denom[]
        searchQuery: '',

        get productsInCategory() {
            return this.activeCategory === null ? []
                : this.products.filter(p => p.categoryId === this.activeCategory);
        },
        get activeDenoms() {
            return this.activeProduct ? (this.denomsByProduct[this.activeProduct] || []) : [];
        },

        selectCategory(id) { this.activeCategory = id; this.activeProduct = null; },
        async selectProduct(id) {
            this.activeProduct = id;
            if (id && !this.denomsByProduct[id]) await this.loadDenoms(id);
        },
        async loadDenoms(productId) {
            try {
                const res = await fetch(`/api/admin/catalog/products/${productId}/denoms`);
                this.denomsByProduct[productId] = await res.json();
            } catch (e) { Alpine.store('toast').error('Gagal memuat denominasi'); }
        },
        // ... carry over: openCategoryModal/saveCategory, openProductModal/saveProduct,
        //     openCreateModal/openEditModal/saveDenom/confirmDelete, sync methods
        //     (openSyncDenoms/syncGroup/toggleGroup/confirmSync), formatRp, dfInfo, loadCompare.
        //     After any denom save/delete: this.loadDenoms(this.activeProduct).
        //     After category/product save: refetch via existing category/product list endpoints
        //     (or splice the returned DTO into this.categories/this.products).
    };
}
```

- [ ] **Step 3: Verify the app compiles and page renders**

Run: `./mvnw -pl satset-core test -Dtest=AdminCatalogPageControllerTest`
Expected: PASS (view still resolves).
Then manual smoke in Task B5 covers interactive behavior.

- [ ] **Step 4: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/index.html
git commit -m "feat(catalog): nested-tab single-page UI (category/product/denom)"
```

---

### Task B3: Add reassign cascade selector to the denom modal

**Files:**
- Modify: `satset-core/src/main/resources/templates/pages/admin/catalog/index.html`

**Interfaces:**
- Consumes: `ProductDTO.categoryId`, `ProductDTO.id`, `ProductDenomDTO.productId`, and `UpdateDenomRequest.productId()` (Phase A).
- Behavior: in the denom edit modal, admin picks Kategori → Produk; `form.productId` = chosen product; sent in the `saveDenom()` PUT body.

- [ ] **Step 1: Add the cascade fields to the denom modal**

Inside the denom modal form (only in EDIT mode — create already scopes by product), add above the pricing divider:

```html
<div class="grid grid-cols-2 gap-4" x-show="editMode">
    <div class="form-control">
        <label class="label"><span class="label-text">Kategori</span></label>
        <select class="select select-bordered select-sm" x-model="form.reassignCategory" :disabled="saving">
            <template x-for="c in categories" :key="c.id">
                <option :value="c.id" x-text="c.name"></option>
            </template>
        </select>
    </div>
    <div class="form-control">
        <label class="label"><span class="label-text">Produk</span></label>
        <select class="select select-bordered select-sm" x-model="form.productId" :disabled="saving">
            <template x-for="p in products.filter(p => p.categoryId === form.reassignCategory)" :key="p.id">
                <option :value="p.id" x-text="p.name"></option>
            </template>
        </select>
    </div>
</div>
```

- [ ] **Step 2: Seed cascade state in `openEditModal(d)` and include in payload**

In `openEditModal(d)`, set the cascade defaults from the denom's current product:

```javascript
const prod = this.products.find(p => p.id === d.productId);
this.form.productId = d.productId;
this.form.reassignCategory = prod ? prod.categoryId : null;
```

`saveDenom()` already sends `JSON.stringify(this.form)`, so `productId` is included automatically. When the reassigned product's category differs, after a successful PUT re-derive tabs: reload the denom lists for both the old and new product (`this.loadDenoms(oldProductId)` and `this.loadDenoms(newProductId)`), so the denom disappears from the old tab and appears under the new one on next view.

- [ ] **Step 3: Verify**

Run: `./mvnw -pl satset-core test -Dtest=AdminCatalogPageControllerTest`
Expected: PASS. Interactive reassign verified in Task B5.

- [ ] **Step 4: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/index.html
git commit -m "feat(catalog): denom reassign cascade selector in edit modal"
```

---

### Task B4: Retire the three old pages and routes

**Files:**
- Remove (`git rm`): `templates/pages/admin/catalog/categories.html`, `products.html`, `denoms.html`
- Modify: `AdminCatalogPageController.java` — delete `categoriesPage`, `productsPage`, `denomsPage`
- Modify: any template still linking to the old routes

- [ ] **Step 1: Find inbound links to the old routes**

Run: `grep -rn "/admin/catalog/categories\|/admin/catalog/products" satset-core/src/main/resources/templates`
Repoint any hit (sidebar nav, breadcrumbs, buttons) to `/admin/catalog`. The nested tabs replace category/product drill-down navigation.

- [ ] **Step 2: Delete the old page routes**

In `AdminCatalogPageController`, remove the `@GetMapping("/categories")`, `@GetMapping("/products")`, and `@GetMapping("/products/{productId}/denoms")` methods (lines 44-101). Keep only `catalogRoot(Model)`. Remove now-unused imports flagged by the compiler.

- [ ] **Step 3: `git rm` the old templates**

```bash
git rm satset-core/src/main/resources/templates/pages/admin/catalog/categories.html \
       satset-core/src/main/resources/templates/pages/admin/catalog/products.html \
       satset-core/src/main/resources/templates/pages/admin/catalog/denoms.html
```

- [ ] **Step 4: Prune obsolete controller tests**

In `AdminCatalogPageControllerTest`, remove any test asserting the deleted `/categories`, `/products`, `/products/{id}/denoms` page routes. Keep the new single-page test from B1.

- [ ] **Step 5: Run the full catalog test suite**

Run: `./mvnw -pl satset-core test -Dtest="*Catalog*,DenomDomainServiceTest,ProductDomainServiceTest,CategoryDomainServiceTest"`
Expected: PASS. No references to deleted routes/templates.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(catalog): retire 3 old catalog pages, single page only"
```

---

### Task B5: Manual verification (run the app)

- [ ] **Step 1: Boot the app** (dev profile, DB + Keycloak up per project setup).

- [ ] **Step 2: Verify the nested-tab flow at `/admin/catalog`:**
  - Category tab bar shows `Semua` + each category; `Semua` panel = flat products table.
  - Click a category → product subtab bar appears with `Semua` + products; `Semua` = flat denoms in that category.
  - Click a product → its denom table loads (lazy, one fetch, cached on re-click).
  - All switching is client-side — no full page reload.

- [ ] **Step 3: Verify edits:**
  - Edit a category (name/type) → persists.
  - Edit a product, change its category → product moves to the other category's tab.
  - Edit a denom, use the cascade selector to move it to a product in another category → after save, the denom leaves the old product tab and appears under the new product/category.

- [ ] **Step 4: Verify sync** — `Sync Denom DF` preview + apply still works on a product's denom panel.

- [ ] **Step 5: Full regression**

Run: `./mvnw -pl satset-core test`
Expected: PASS (no regressions).

---

## Self-Review Notes

- **Spec coverage:** single-page nested tabs (B1-B2), "Semua" shortcuts at both levels (B2 panels), reuse category/product edit (B2 modals), reassign denom incl. new backend field (A1-A2, B3), lazy denom load (B2 `loadDenoms` cache), retire old pages (B4). All spec sections mapped.
- **Non-goals honored:** DF sync untouched (product-first), `productId` stays NOT NULL, reassign is optional (nullable field, no move when null/same).
- **Type consistency:** `UpdateDenomRequest.productId()` (A1) consumed by `DenomDomainService.update` (A2) and sent by `saveDenom()` (B3); `activeCategory`/`activeProduct`/`denomsByProduct` names consistent across B2-B3; `ProductDTO.categoryId` used by both `productsInCategory` and the cascade filter.
