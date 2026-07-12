# Catalog Denom-Centric Admin — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the admin catalog page (`/admin/catalog`) denom-centric: one always-on denom table filtered by category/product tabs, with inline category/product creation, auto-pruned tabs, a global `Harga Suplier` column with a stale-price flag, and a read-only DF sync-all preview.

**Architecture:** Backend adds an aggregate denom read + SSR seed in the `catalog` slice, and three small `supplier`-slice additions (a cache-timestamped price snapshot, a `supplier-prices` map endpoint, a `sync/all/preview` summary). The frontend rewrites `pages/admin/catalog/index.html`'s Alpine component so the denom table is the single content, tabs are pure filters derived from the denom set, and the denom modal is the only create door (category/product via combobox → reuse existing POST endpoints).

**Tech Stack:** Spring Boot 4, Java 25, Hibernate/JPA, Thymeleaf + Alpine.js + Tailwind/daisyUI, JUnit 5 + Mockito + AssertJ, MockMvc.

## Global Constraints

- **Language/UI copy:** Bahasa Indonesia, plain.
- **No schema change:** `Category`, `Products`, `ProductDenoms` entities/tables unchanged. Buyer catalog, purchase page, transaction flow untouched.
- **DTO:** no new `ProductDenomDTO` fields — client resolves category/product from the `initialProducts` / `initialCategories` maps via `d.productId`.
- **Money screens:** clear amounts/states; visible focus; `prefers-reduced-motion` fallback on any new animation; touch targets ≥44px.
- **Errors:** never expose `e.getMessage()` to the client — `log.error()` server-side, generic toast to user.
- **DF rate limit:** the DF pricelist is one Caffeine cache entry (`digiflazzPriceList`, 5h). Do NOT add a second cache key that triggers a second DF fetch.
- **Git deletion:** use `git rm` for file removal/rename.
- **TDD:** backend tasks are red→green→refactor. Frontend (Alpine, no JS test harness here) uses MockMvc where a controller is involved + explicit manual browser verification.
- **Commit** after each task.
- **Verify command:** `./mvnw -q -pl satset-core test -Dtest=<TestClass>` for a single test class; full slice run before marking done.

---

## File Structure

**Backend — `catalog` slice**
- `repository/DenomRepository.java` (modify) — add `findAllByOrderBySortOrder()`.
- `service/DenomDomainService.java` (modify) — add `findAllForAdmin()`.
- `web/AdminCatalogController.java` (modify) — add `GET /denoms`.
- `web/AdminCatalogPageController.java` (modify) — inject `DenomDomainService`, seed `initialDenoms`.

**Backend — `supplier` slice**
- `model/PriceListSnapshot.java` (create) — `record PriceListSnapshot(List<PriceListItem> items, LocalDateTime fetchedAt)`.
- `client/DigiflazzClient.java` (modify) — cache `fetchSnapshot()`, delegate `fetchPriceList()`.
- `service/SupplierPriceView.java` (create) — `record SupplierPriceView(LocalDateTime fetchedAt, Map<String,Long> prices)`.
- `service/SyncAllPreview.java` (create) — `record SyncAllPreview(List<String> newCategories, List<String> newProducts, List<String> newDenoms, List<String> priceChanges)`.
- `service/CatalogSyncService.java` (modify) — add `supplierPrices()`, `syncAllPreview()`.
- `web/CatalogSyncController.java` (modify) — add `GET /supplier-prices`, `GET /sync/all/preview`.

**Frontend**
- `resources/templates/pages/admin/catalog/index.html` (modify) — Alpine component rewrite + markup changes.

**Tests**
- `test/.../catalog/web/AdminCatalogControllerTest.java` (modify) — `GET /denoms`.
- `test/.../catalog/service/DenomDomainServiceTest.java` (modify) — `findAllForAdmin()`.
- `test/.../catalog/web/AdminCatalogPageControllerTest.java` (modify) — `initialDenoms` seeded + new mock.
- `test/.../supplier/service/CatalogSyncServiceTest.java` (create or modify) — snapshot/supplierPrices/syncAllPreview.
- `test/.../supplier/client/DigiflazzClientTest.java` (create or modify, if present) — delegate behavior.

---

## Phase A — Backend: aggregate denom read + SSR seed

### Task 1: Aggregate denom endpoint

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/repository/DenomRepository.java`
- Modify: `satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java`
- Modify: `satset-core/src/main/java/com/satset/catalog/web/AdminCatalogController.java`
- Test: `satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java`
- Test: `satset-core/src/test/java/com/satset/catalog/web/AdminCatalogControllerTest.java`

**Interfaces:**
- Consumes: `DenomRepository extends JpaRepository<ProductDenoms, UUID>`; `CatalogDtoMapper.toDenomDTO(ProductDenoms) → ProductDenomDTO`; `manageDenomsUseCase` field in `AdminCatalogController` (type `DenomDomainService`).
- Produces: `DenomDomainService.findAllForAdmin() → List<ProductDenoms>`; `GET /api/admin/catalog/denoms → List<ProductDenomDTO>`.

- [ ] **Step 1: Write the failing service test**

In `DenomDomainServiceTest.java`, add (match the file's existing mock setup for `denomRepository`):

```java
@Test
void findAllForAdmin_returnsRepositoryOrder() {
    ProductDenoms a = new ProductDenoms();
    a.setCode("A"); a.setSortOrder(0);
    ProductDenoms b = new ProductDenoms();
    b.setCode("B"); b.setSortOrder(1);
    when(denomRepository.findAllByOrderBySortOrder()).thenReturn(List.of(a, b));

    List<ProductDenoms> result = service.findAllForAdmin();

    assertThat(result).containsExactly(a, b);
}
```

- [ ] **Step 2: Run it, verify it fails to compile / fails**

Run: `./mvnw -q -pl satset-core test -Dtest=DenomDomainServiceTest`
Expected: compile error — `findAllByOrderBySortOrder` and `findAllForAdmin` do not exist.

- [ ] **Step 3: Add the repository method**

In `DenomRepository.java`, alongside the existing `findByProductIdOrderBySortOrder`:

```java
List<ProductDenoms> findAllByOrderBySortOrder();
```

- [ ] **Step 4: Add the service method**

In `DenomDomainService.java`, in the browse/read-only section:

```java
/** All denoms incl. deleted, for the admin aggregate view (client greys deleted). */
public List<ProductDenoms> findAllForAdmin() {
    return denomRepository.findAllByOrderBySortOrder();
}
```

- [ ] **Step 5: Run the service test, verify pass**

Run: `./mvnw -q -pl satset-core test -Dtest=DenomDomainServiceTest`
Expected: PASS.

- [ ] **Step 6: Write the failing controller test**

In `AdminCatalogControllerTest.java` (follow the file's existing MockMvc + mocked `manageDenomsUseCase` pattern):

```java
@Test
void listAllDenoms_returnsAll() throws Exception {
    ProductDenoms d = new ProductDenoms();
    d.setId(UUID.randomUUID());
    d.setCode("TSEL5");
    d.setName("Telkomsel 5rb");
    d.setDenomType(DenomType.FIXED_DENOM);
    d.setProductId(UUID.randomUUID());
    when(manageDenomsUseCase.findAllForAdmin()).thenReturn(List.of(d));

    mockMvc.perform(get("/api/admin/catalog/denoms"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].code").value("TSEL5"));
}
```

- [ ] **Step 7: Run it, verify it fails**

Run: `./mvnw -q -pl satset-core test -Dtest=AdminCatalogControllerTest`
Expected: FAIL — 404 (no mapping) or compile error.

- [ ] **Step 8: Add the controller endpoint**

In `AdminCatalogController.java`, in the Denoms section (just above `@GetMapping("/products/{productId}/denoms")`):

```java
@GetMapping("/denoms")
@PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
public ResponseEntity<List<ProductDenomDTO>> listAllDenoms() {
    List<ProductDenomDTO> dtos = manageDenomsUseCase.findAllForAdmin().stream()
            .map(CatalogDtoMapper::toDenomDTO).toList();
    return ResponseEntity.ok(dtos);
}
```

- [ ] **Step 9: Run both test classes, verify pass**

Run: `./mvnw -q -pl satset-core test -Dtest=DenomDomainServiceTest,AdminCatalogControllerTest`
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/repository/DenomRepository.java \
        satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java \
        satset-core/src/main/java/com/satset/catalog/web/AdminCatalogController.java \
        satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java \
        satset-core/src/test/java/com/satset/catalog/web/AdminCatalogControllerTest.java
git commit -m "feat(catalog): aggregate GET /denoms + findAllForAdmin"
```

---

### Task 2: SSR-seed all denoms on the page

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/web/AdminCatalogPageController.java`
- Test: `satset-core/src/test/java/com/satset/catalog/web/AdminCatalogPageControllerTest.java`

**Interfaces:**
- Consumes: `DenomDomainService.findAllForAdmin()` (Task 1); `CatalogDtoMapper.toDenomDTO`.
- Produces: model attribute `initialDenoms` (`List<ProductDenomDTO>`) on `GET /admin/catalog`.

- [ ] **Step 1: Update the failing page test**

In `AdminCatalogPageControllerTest.java`: add a `@Mock private DenomDomainService manageDenomsUseCase;`, pass it to the controller constructor in `setUp()`, stub it, and assert the new attribute:

```java
// in setUp(), after the existing when(...) stubs:
when(manageDenomsUseCase.findAllForAdmin()).thenReturn(List.of());

// controller construction becomes:
new AdminCatalogPageController(manageCategoriesUseCase, manageProductsUseCase, manageDenomsUseCase)

// in catalogRoot_rendersSinglePage_withSeededData():
.andExpect(model().attributeExists("initialDenoms"));
```

- [ ] **Step 2: Run it, verify it fails**

Run: `./mvnw -q -pl satset-core test -Dtest=AdminCatalogPageControllerTest`
Expected: FAIL — constructor arity / missing attribute.

- [ ] **Step 3: Inject the service and seed the attribute**

In `AdminCatalogPageController.java`:

```java
// add import:
import com.satset.catalog.dto.ProductDenomDTO;
import com.satset.catalog.service.DenomDomainService;

// add field + constructor param:
private final DenomDomainService manageDenomsUseCase;

public AdminCatalogPageController(CategoryDomainService manageCategoriesUseCase,
                                  ProductDomainService manageProductsUseCase,
                                  DenomDomainService manageDenomsUseCase) {
    this.manageCategoriesUseCase = manageCategoriesUseCase;
    this.manageProductsUseCase = manageProductsUseCase;
    this.manageDenomsUseCase = manageDenomsUseCase;
}

// in catalogRoot(), after initialProducts:
List<ProductDenomDTO> denoms = manageDenomsUseCase.findAllForAdmin().stream()
        .map(CatalogDtoMapper::toDenomDTO).toList();
model.addAttribute("initialDenoms", denoms);
```

- [ ] **Step 4: Run it, verify pass**

Run: `./mvnw -q -pl satset-core test -Dtest=AdminCatalogPageControllerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/web/AdminCatalogPageController.java \
        satset-core/src/test/java/com/satset/catalog/web/AdminCatalogPageControllerTest.java
git commit -m "feat(catalog): SSR-seed initialDenoms on catalog page"
```

---

## Phase B — Supplier: price snapshot + endpoints

### Task 3: Cache-timestamped price snapshot

**Files:**
- Create: `satset-core/src/main/java/com/satset/supplier/model/PriceListSnapshot.java`
- Modify: `satset-core/src/main/java/com/satset/supplier/client/DigiflazzClient.java`
- Test: `satset-core/src/test/java/com/satset/supplier/client/DigiflazzClientTest.java` (create if absent)

**Interfaces:**
- Produces: `record PriceListSnapshot(List<PriceListItem> items, LocalDateTime fetchedAt)`; `DigiflazzClient.fetchSnapshot() → PriceListSnapshot` (cached under `digiflazzPriceList`); `fetchPriceList()` now delegates to `fetchSnapshot().items()`.

- [ ] **Step 1: Create the record**

`PriceListSnapshot.java`:

```java
package com.satset.supplier.model;

import java.time.LocalDateTime;
import java.util.List;

/** Cached DF pricelist plus the moment the cache entry was filled. */
public record PriceListSnapshot(List<PriceListItem> items, LocalDateTime fetchedAt) {}
```

- [ ] **Step 2: Move the cache onto `fetchSnapshot()`, delegate `fetchPriceList()`**

In `DigiflazzClient.java`: remove `@Cacheable` from `fetchPriceList()`, rename its body into a new cached `fetchSnapshot()`, and have `fetchPriceList()` delegate. `fetchedAt` is stamped only on a cache miss (i.e. when the method body actually runs), so a cache hit preserves the original timestamp.

```java
import com.satset.supplier.model.PriceListSnapshot;
import java.time.LocalDateTime;

/** Cached snapshot (items + fill time). 5h TTL; timestamp = cache-fill moment. */
@Cacheable(value = "digiflazzPriceList", cacheManager = "digiflazzCacheManager")
public PriceListSnapshot fetchSnapshot() {
    return new PriceListSnapshot(doFetchPriceList(), LocalDateTime.now());
}

/** Items only — delegates to the cached snapshot (no extra DF hit). */
public List<PriceListItem> fetchPriceList() {
    return fetchSnapshot().items();
}

// rename the current @Cacheable method body to a private helper:
private List<PriceListItem> doFetchPriceList() {
    var req = new PriceListRequest("prepaid", username, sign("pricelist"));
    // ... unchanged body of the old fetchPriceList() ...
}
```

`// ponytail: fetchPriceList() delegates so all existing callers are untouched; only fetchSnapshot() is cached, so still one DF hit / 5h.`

- [ ] **Step 3: Write the delegate test**

In `DigiflazzClientTest.java` — if the class exists, add; otherwise create it with a `@Mock RestClient` or a Mockito-spy approach. Minimal spy test (no network):

```java
@Test
void fetchPriceList_returnsSnapshotItems() {
    DigiflazzClient client = spy(new DigiflazzClient(mock(RestClient.class), "u", "url", "k"));
    PriceListItem item = new PriceListItem("Tsel 5rb", "Pulsa", "Telkomsel", "tsel5", 5000L, true, "ok", "S");
    doReturn(new PriceListSnapshot(List.of(item), LocalDateTime.now())).when(client).fetchSnapshot();

    assertThat(client.fetchPriceList()).containsExactly(item);
}
```

- [ ] **Step 4: Run it, verify pass**

Run: `./mvnw -q -pl satset-core test -Dtest=DigiflazzClientTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/supplier/model/PriceListSnapshot.java \
        satset-core/src/main/java/com/satset/supplier/client/DigiflazzClient.java \
        satset-core/src/test/java/com/satset/supplier/client/DigiflazzClientTest.java
git commit -m "feat(supplier): cache DF pricelist as snapshot with fetchedAt"
```

---

### Task 4: `supplier-prices` endpoint (global SKU→cost map + date)

**Files:**
- Create: `satset-core/src/main/java/com/satset/supplier/service/SupplierPriceView.java`
- Modify: `satset-core/src/main/java/com/satset/supplier/service/CatalogSyncService.java`
- Modify: `satset-core/src/main/java/com/satset/supplier/web/CatalogSyncController.java`
- Test: `satset-core/src/test/java/com/satset/supplier/service/CatalogSyncServiceTest.java`

**Interfaces:**
- Consumes: `DigiflazzClient.fetchSnapshot()` (Task 3).
- Produces: `record SupplierPriceView(LocalDateTime fetchedAt, Map<String,Long> prices)`; `CatalogSyncService.supplierPrices() → SupplierPriceView`; `GET /api/admin/catalog/supplier-prices`.

- [ ] **Step 1: Create the view record**

`SupplierPriceView.java`:

```java
package com.satset.supplier.service;

import java.time.LocalDateTime;
import java.util.Map;

/** Global SKU(upper) -> DF cost map plus the cache fill time, for the admin Harga Suplier column. */
public record SupplierPriceView(LocalDateTime fetchedAt, Map<String, Long> prices) {}
```

- [ ] **Step 2: Write the failing service test**

In `CatalogSyncServiceTest.java` (create the class if absent, mocking `DigiflazzClient digiflazz`, `CategoryDomainService`, `ProductDomainService`, `DenomDomainService`):

```java
@Test
void supplierPrices_mapsSkuUpperToCost_lowestWins() {
    LocalDateTime at = LocalDateTime.of(2026, 7, 12, 8, 0);
    when(digiflazz.fetchSnapshot()).thenReturn(new PriceListSnapshot(List.of(
            new PriceListItem("Tsel 5rb", "Pulsa", "Telkomsel", "tsel5", 5200L, true, "ok", "S1"),
            new PriceListItem("Tsel 5rb", "Pulsa", "Telkomsel", "tsel5", 5000L, true, "ok", "S2")
    ), at));

    SupplierPriceView v = service.supplierPrices();

    assertThat(v.fetchedAt()).isEqualTo(at);
    assertThat(v.prices()).containsEntry("TSEL5", 5000L); // lowest price wins
}
```

- [ ] **Step 3: Run it, verify it fails**

Run: `./mvnw -q -pl satset-core test -Dtest=CatalogSyncServiceTest`
Expected: FAIL — `supplierPrices` undefined.

- [ ] **Step 4: Implement `supplierPrices()`**

In `CatalogSyncService.java`:

```java
import com.satset.supplier.model.PriceListSnapshot;

/** Global SKU(upper) -> cheapest DF cost, from the cached snapshot (no forced fetch). */
public SupplierPriceView supplierPrices() {
    PriceListSnapshot snap = digiflazz.fetchSnapshot();
    Map<String, Long> prices = new LinkedHashMap<>();
    for (PriceListItem it : snap.items()) {
        prices.merge(it.buyerSkuCode().toUpperCase(), it.price(), Math::min);
    }
    return new SupplierPriceView(snap.fetchedAt(), prices);
}
```

- [ ] **Step 5: Add the controller endpoint**

In `CatalogSyncController.java`:

```java
@GetMapping("/supplier-prices")
@PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
public SupplierPriceView supplierPrices() { return sync.supplierPrices(); }
```

(Match the existing import for `OmniConstants` / permission constant used by the other endpoints in this controller. If the class uses a different view-permission constant, reuse that one.)

- [ ] **Step 6: Run it, verify pass**

Run: `./mvnw -q -pl satset-core test -Dtest=CatalogSyncServiceTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add satset-core/src/main/java/com/satset/supplier/service/SupplierPriceView.java \
        satset-core/src/main/java/com/satset/supplier/service/CatalogSyncService.java \
        satset-core/src/main/java/com/satset/supplier/web/CatalogSyncController.java \
        satset-core/src/test/java/com/satset/supplier/service/CatalogSyncServiceTest.java
git commit -m "feat(supplier): GET /supplier-prices global SKU->cost map with date"
```

---

### Task 5: `sync/all/preview` read-only summary

**Files:**
- Create: `satset-core/src/main/java/com/satset/supplier/service/SyncAllPreview.java`
- Modify: `satset-core/src/main/java/com/satset/supplier/service/CatalogSyncService.java`
- Modify: `satset-core/src/main/java/com/satset/supplier/web/CatalogSyncController.java`
- Test: `satset-core/src/test/java/com/satset/supplier/service/CatalogSyncServiceTest.java`

**Interfaces:**
- Consumes: existing `previewCategories()`, `previewProducts(UUID)`, `reconcileForProduct(UUID)`, `categoryService.findAllForAdmin()`, `productService.findByCategoryForAdmin(UUID)`.
- Produces: `record SyncAllPreview(List<String> newCategories, List<String> newProducts, List<String> newDenoms, List<String> priceChanges)`; `CatalogSyncService.syncAllPreview() → SyncAllPreview`; `GET /api/admin/catalog/sync/all/preview`.

- [ ] **Step 1: Create the record**

`SyncAllPreview.java`:

```java
package com.satset.supplier.service;

import java.util.List;

/** Read-only summary of what a full syncAll() would change. Labels only; no selection keys. */
public record SyncAllPreview(
        List<String> newCategories,
        List<String> newProducts,
        List<String> newDenoms,
        List<String> priceChanges) {}
```

- [ ] **Step 2: Write the failing service test**

In `CatalogSyncServiceTest.java`:

```java
@Test
void syncAllPreview_listsNewCategories() {
    // DF has a category the catalog lacks -> previewCategories() yields an ADD
    when(digiflazz.fetchPriceList()).thenReturn(List.of(
            new PriceListItem("Tsel 5rb", "Pulsa", "Telkomsel", "tsel5", 5000L, true, "ok", "S")));
    when(categoryService.findAllForAdmin()).thenReturn(List.of()); // nothing yet

    SyncAllPreview p = service.syncAllPreview();

    assertThat(p.newCategories()).contains("Pulsa");
}
```

- [ ] **Step 3: Run it, verify it fails**

Run: `./mvnw -q -pl satset-core test -Dtest=CatalogSyncServiceTest`
Expected: FAIL — `syncAllPreview` undefined.

- [ ] **Step 4: Implement `syncAllPreview()`**

In `CatalogSyncService.java` — reuse the existing per-level preview methods; only ADD categories/products count as "new", denom BARU = new, NAIK/TURUN = price change:

```java
import com.satset.supplier.model.CompareStatus;

/** Read-only: what a full syncAll() would add/change. Only over categories/products that already exist
 *  (denoms of brand-new products appear after the next sync — see plan/spec note). */
public SyncAllPreview syncAllPreview() {
    List<String> newCategories = previewCategories().stream()
            .filter(i -> i.action() == com.satset.supplier.model.SyncAction.ADD)
            .map(SyncPreviewItem::label).toList();

    List<String> newProducts = new ArrayList<>();
    List<String> newDenoms = new ArrayList<>();
    List<String> priceChanges = new ArrayList<>();
    for (Category c : categoryService.findAllForAdmin()) {
        if (c.isDeleted()) continue;
        previewProducts(c.getId()).stream()
                .filter(i -> i.action() == com.satset.supplier.model.SyncAction.ADD)
                .forEach(i -> newProducts.add(c.getName() + " / " + i.label()));
        for (Products p : productService.findByCategoryForAdmin(c.getId())) {
            if (p.isDeleted()) continue;
            for (PriceCompareRow r : reconcileForProduct(p.getId())) {
                if (r.status() == CompareStatus.BARU) newDenoms.add(p.getName() + " / " + r.productName());
                else if (r.status() == CompareStatus.NAIK || r.status() == CompareStatus.TURUN)
                    priceChanges.add(p.getName() + " / " + r.buyerSku());
            }
        }
    }
    return new SyncAllPreview(newCategories, newProducts, newDenoms, priceChanges);
}
```

- [ ] **Step 5: Add the controller endpoint**

In `CatalogSyncController.java`:

```java
@GetMapping("/sync/all/preview")
@PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATALOG + "')")
public SyncAllPreview syncAllPreview() { return sync.syncAllPreview(); }
```

(Reuse whatever manage-permission constant `POST /sync/all` already uses in this controller.)

- [ ] **Step 6: Run it, verify pass**

Run: `./mvnw -q -pl satset-core test -Dtest=CatalogSyncServiceTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add satset-core/src/main/java/com/satset/supplier/service/SyncAllPreview.java \
        satset-core/src/main/java/com/satset/supplier/service/CatalogSyncService.java \
        satset-core/src/main/java/com/satset/supplier/web/CatalogSyncController.java \
        satset-core/src/test/java/com/satset/supplier/service/CatalogSyncServiceTest.java
git commit -m "feat(supplier): GET /sync/all/preview read-only summary"
```

---

## Phase C — Frontend: denom-centric page

All tasks modify `satset-core/src/main/resources/templates/pages/admin/catalog/index.html`. No JS unit harness exists here, so each task ends with **manual browser verification** against the running dev server (`http://localhost:8080/admin/catalog`, already up). Restart the app if a controller/template change needs a reload. Commit after each task.

**Reference — the Alpine component is `catalogManager()` in the `<script th:inline="javascript">` block; `INITIAL_*` consts are injected at the top.**

### Task 6: State + data flow (denoms seed, refetch on toggle, scope filter)

**Files:**
- Modify: `index.html` — `<script>` const block + `catalogManager()` state/getters/nav; add `initialDenoms` inline.

**Interfaces:**
- Consumes: `GET /api/admin/catalog/denoms` (Task 1); seeded `initialDenoms` (Task 2).
- Produces (used by later tasks): `this.denoms`, getters `filteredDenoms`, `denomCategoryIds`, `denomProductIds`; helpers `productById(id)`, `catNameOf(productId)`, `prodNameOf(productId)`, `catIdOf(productId)`; `async refreshDenoms()`.

- [ ] **Step 1: Add the seed const**

In the const block near `INITIAL_PRODUCTS`:

```javascript
const INITIAL_DENOMS = /*[[${initialDenoms}]]*/ [];
```

- [ ] **Step 2: Replace per-product denom state with the aggregate array**

In `catalogManager()` return object: add `denoms: INITIAL_DENOMS,` and `denomsLoading: false,`. Remove `denomsByProduct: {},` and `compareByProduct: {},` (compare is superseded by Task 7's supplier map).

- [ ] **Step 3: Add resolve helpers + rewrite scope getters**

Replace the `activeDenoms` / `filteredDenoms` getters with:

```javascript
productById(id) { return this.products.find(p => p.id === id) || null; },
catIdOf(productId) { const p = this.productById(productId); return p ? p.categoryId : null; },
prodNameOf(productId) { const p = this.productById(productId); return p ? p.name : '-'; },
catNameOf(productId) { return this.catName(this.catIdOf(productId)); },

get scopedDenoms() {
    if (this.activeProduct !== null) return this.denoms.filter(d => d.productId === this.activeProduct);
    if (this.activeCategory !== null) return this.denoms.filter(d => this.catIdOf(d.productId) === this.activeCategory);
    return this.denoms;
},
get filteredDenoms() {
    const list = this.scopedDenoms;
    if (!this.searchQuery) return list;
    const q = this.searchQuery.toLowerCase();
    return list.filter(d =>
        (d.code || '').toLowerCase().includes(q) ||
        (d.name || '').toLowerCase().includes(q) ||
        (d.denomType || '').toLowerCase().includes(q) ||
        this.prodNameOf(d.productId).toLowerCase().includes(q) ||
        this.catNameOf(d.productId).toLowerCase().includes(q)
    );
},
```

- [ ] **Step 4: Add `refreshDenoms()` and wire nav to it**

Replace `selectCategory`, `selectProduct`, `openProductDenoms` and add `refreshDenoms`:

```javascript
async refreshDenoms() {
    this.denomsLoading = true;
    try {
        const res = await fetch('/api/admin/catalog/denoms');
        this.denoms = await res.json();
    } catch (e) {
        Alpine.store('toast').error('Gagal memuat denominasi');
    } finally {
        this.denomsLoading = false;
    }
},
selectCategory(id) {
    this.activeCategory = id;
    this.activeProduct = null;
    this.searchQuery = '';
    this.refreshDenoms();
},
selectProduct(id) {
    this.activeProduct = id;
    this.searchQuery = '';
    this.refreshDenoms();
},
openProductDenoms(prod) {
    this.activeCategory = prod.categoryId;
    this.selectProduct(prod.id);
},
```

- [ ] **Step 5: Remove dead denom-loading code**

Delete `loadDenoms()`, `loadCompare()`, `dfInfo()`, and the `activeDenoms` getter references. (Task 7 replaces DF display; Task 11 replaces sync.) Keep `formatRp`, `activeProductName`, `catName`.

- [ ] **Step 6: Manual verification**

Restart app if needed. Load `http://localhost:8080/admin/catalog`. In DevTools console run:
```
document.querySelector('[x-data]').__x.$data.denoms.length
```
Expected: > 0 (seeded). Switch a category tab → Network shows `GET /api/admin/catalog/denoms`; `filteredDenoms` narrows to that category. (The table itself is wired in Task 7; for now verify state via console.)

- [ ] **Step 7: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/index.html
git commit -m "feat(catalog-ui): aggregate denom state + scope filter + refetch-on-toggle"
```

---

### Task 7: Denom table — columns + Harga Suplier (map, date, diff flag)

**Files:**
- Modify: `index.html` — denom `<table>` panel; component `init()` + supplier-price state.

**Interfaces:**
- Consumes: `GET /api/admin/catalog/supplier-prices` (Task 4) → `{ fetchedAt, prices }`; helpers from Task 6.
- Produces: `this.supplierPrices` (`{SKU: cost}`), `this.supplierPricesAt`; helpers `supplierCost(code)`, `supplierDiffers(d)`.

- [ ] **Step 1: Add supplier-price state + loader in `init()`**

In `catalogManager()`:

```javascript
supplierPrices: {},
supplierPricesAt: null,

init() { this.loadSupplierPrices(); },

async loadSupplierPrices() {
    try {
        const res = await fetch('/api/admin/catalog/supplier-prices');
        const v = await res.json();
        this.supplierPrices = v.prices || {};
        this.supplierPricesAt = v.fetchedAt || null;
    } catch (e) { /* diam: kolom suplier opsional */ }
},
supplierCost(code) {
    const c = this.supplierPrices[(code || '').toUpperCase()];
    return (c === undefined) ? null : c;
},
supplierDiffers(d) {
    const c = this.supplierCost(d.code);
    return c != null && d.basePrice != null && Number(c) !== Number(d.basePrice);
},
```

- [ ] **Step 2: Make the denom table always visible + always render `filteredDenoms`**

Remove the `x-show="activeProduct !== null"` wrapper from the denom panel and the whole products-table panel (`x-show="activeProduct === null"` block) — the products table is removed in Task 10; here, make the denom `<div class="card ...">` render unconditionally and its `x-for` iterate `filteredDenoms`. Update the header `<h2>` to show the current scope:

```html
<h2 class="text-lg font-semibold"
    x-text="activeProduct !== null ? ('Denom: ' + activeProductName)
            : activeCategory !== null ? ('Denom: ' + catName(activeCategory))
            : 'Semua Denom'"></h2>
```

- [ ] **Step 3: Rename price headers + add Kategori/Produk/Harga Suplier columns**

In the denom `<thead>`, set the vocabulary and add columns (order: Code, Nama, Kategori, Produk, Tipe, Nominal, Harga Jual, Harga Beli, Harga Suplier, Sort, Status, Aksi):

```html
<th>Code</th><th>Nama</th><th>Kategori</th><th>Produk</th><th>Tipe</th>
<th class="text-right">Nominal</th>
<th class="text-right">Harga Jual</th>
<th class="text-right">Harga Beli</th>
<th class="text-right">
  Harga Suplier
  <span x-show="supplierPricesAt" class="block text-[10px] font-normal text-base-content/40"
        x-text="supplierPricesAt ? ('upd ' + new Date(supplierPricesAt).toLocaleDateString('id-ID')) : ''"></span>
</th>
<th class="text-center">Sort</th><th class="text-center">Status</th><th class="text-center">Aksi</th>
```

- [ ] **Step 4: Add the matching `<td>` cells in the row template**

Inside `x-for="d in filteredDenoms"`, add Kategori + Produk after the Nama cell, keep Harga Jual (`price`) + Harga Beli (`basePrice`), and replace the old Harga DF cell with Harga Suplier + diff flag:

```html
<td><span class="badge badge-outline badge-sm" x-text="catNameOf(d.productId)"></span></td>
<td x-text="prodNameOf(d.productId)"></td>
...
<td class="text-right font-mono" x-text="formatRp(d.price)"></td>       <!-- Harga Jual -->
<td class="text-right font-mono" x-text="formatRp(d.basePrice)"></td>   <!-- Harga Beli -->
<td class="text-right font-mono">
  <template x-if="supplierCost(d.code) !== null">
    <span :class="supplierDiffers(d) ? 'text-warning font-semibold' : ''">
      <span x-text="formatRp(supplierCost(d.code))"></span>
      <span x-show="supplierDiffers(d)" x-text="Number(supplierCost(d.code)) > Number(d.basePrice ?? 0) ? '▲' : '▼'"></span>
    </span>
  </template>
  <template x-if="supplierCost(d.code) === null"><span class="text-base-content/40">-</span></template>
</td>
```

Update the empty-row `colspan` to the new column count (12).

- [ ] **Step 5: Manual verification**

Restart app. Load the page. Expected: denom table shows for Semua/Semua with **Kategori** + **Produk** filled; **Harga Suplier** shows a number where the SKU exists in DF (else `-`), with a `▲/▼` warning-coloured mark when it differs from Harga Beli; the "upd <date>" caption appears under the header. Switch a category → rows narrow. Contrast check: warning text on the row background is legible.

- [ ] **Step 6: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/index.html
git commit -m "feat(catalog-ui): denom table columns + global Harga Suplier with diff flag"
```

---

### Task 8: Auto-prune filter tabs

**Files:**
- Modify: `index.html` — category pill `x-for`, product subtab `x-for`.

**Interfaces:**
- Consumes: `this.denoms`, `catIdOf` (Task 6).
- Produces: getters `tabCategories`, `tabProducts`.

- [ ] **Step 1: Add prune getters**

```javascript
get denomCategoryIds() {
    return new Set(this.denoms.filter(d => !d.deleted).map(d => this.catIdOf(d.productId)).filter(Boolean));
},
get denomProductIds() {
    return new Set(this.denoms.filter(d => !d.deleted).map(d => d.productId));
},
get tabCategories() {
    const ids = this.denomCategoryIds;
    return this.categories.filter(c => ids.has(c.id));
},
get tabProducts() {
    if (this.activeCategory === null) return [];
    const ids = this.denomProductIds;
    return this.products.filter(p => p.categoryId === this.activeCategory && ids.has(p.id));
},
```

`// ponytail: prune uses the full seeded denom set; a toggle refetch keeps it fresh. Cross-session staleness needs a reload — acceptable for a single-admin tool.`

- [ ] **Step 2: Point the tab `x-for` loops at the pruned getters**

Category pills: change `x-for="c in categories"` → `x-for="c in tabCategories"`.
Product subtabs: change `x-for="p in productsInCategory"` → `x-for="p in tabProducts"`.

- [ ] **Step 3: Manual verification**

Restart app. Load page. Expected: only categories/products that own ≥1 denom appear as tabs. Delete a product's last denom (via the row Hapus) then confirm the refetch drops that product's chip; if its category now has no denoms, that category chip drops too. "Semua" chips remain.

- [ ] **Step 4: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/index.html
git commit -m "feat(catalog-ui): auto-prune filter tabs to categories/products with denoms"
```

---

### Task 9: Combobox denom modal — inline create + orchestration

**Files:**
- Modify: `index.html` — denom modal body (category/product combobox), `denomForm` state, `openCreateModal`/`openEditModal`/`saveDenom`.

**Interfaces:**
- Consumes: `POST /api/admin/catalog/categories`, `POST /api/admin/catalog/products`, `POST /api/admin/catalog/products/{id}/denoms`, `PUT /api/admin/catalog/denoms/{id}`; `reloadCategories()`, `reloadProducts()`, `refreshDenoms()`.
- Produces: nothing consumed by later tasks.

- [ ] **Step 1: Extend `denomForm` + add a code-slug helper**

Replace the `reassignCategory` cascade fields with combobox fields:

```javascript
// denomForm gains:
categoryChoice: '',   // selected existing category id, or '' when typing new
categoryNewName: '',
categoryNewType: '',
categoryNewIcon: '',
productChoice: '',
productNewName: '',
productNewProvider: '',
productNewIcon: '',
```

Add helpers:

```javascript
slugCode(name) { return (name || '').trim().toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_|_$/g, ''); },
get catComboIsNew() { return this.denomForm.categoryChoice === '__new__'; },
get prodComboIsNew() { return this.denomForm.productChoice === '__new__'; },
get comboProducts() {
    const cid = this.denomForm.categoryChoice;
    return (cid && cid !== '__new__') ? this.products.filter(p => p.categoryId === cid && !p.deleted) : [];
},
```

- [ ] **Step 2: Replace the reassign block with the combobox UI**

In the denom modal, replace the `Pindah Produk` cascade `<div>`s with (shown in both create and edit):

```html
<div class="divider text-xs">Kategori &amp; Produk</div>
<div class="grid grid-cols-2 gap-4">
  <div class="form-control">
    <label class="label"><span class="label-text">Kategori</span></label>
    <select class="select select-bordered select-sm" x-model="denomForm.categoryChoice"
            @change="denomForm.productChoice=''" :disabled="denomSaving">
      <option value="">-- Pilih --</option>
      <template x-for="c in activeCategories" :key="c.id"><option :value="c.id" x-text="c.name"></option></template>
      <option value="__new__">+ Kategori baru…</option>
    </select>
    <template x-if="catComboIsNew">
      <div class="mt-2 space-y-2">
        <input type="text" class="input input-bordered input-sm w-full" placeholder="Nama kategori baru"
               x-model="denomForm.categoryNewName" :disabled="denomSaving">
        <select class="select select-bordered select-sm w-full" x-model="denomForm.categoryNewType" :disabled="denomSaving">
          <option value="">-- Tipe --</option>
          <template x-for="t in categoryTypes" :key="t"><option :value="t" x-text="t"></option></template>
        </select>
        <input type="text" class="input input-bordered input-sm w-full" placeholder="Icon URL (opsional)"
               x-model="denomForm.categoryNewIcon" :disabled="denomSaving">
      </div>
    </template>
  </div>
  <div class="form-control">
    <label class="label"><span class="label-text">Produk</span></label>
    <select class="select select-bordered select-sm" x-model="denomForm.productChoice" :disabled="denomSaving || (!denomForm.categoryChoice)">
      <option value="">-- Pilih --</option>
      <template x-for="p in comboProducts" :key="p.id"><option :value="p.id" x-text="p.name"></option></template>
      <option value="__new__">+ Produk baru…</option>
    </select>
    <template x-if="prodComboIsNew">
      <div class="mt-2 space-y-2">
        <input type="text" class="input input-bordered input-sm w-full" placeholder="Nama produk baru"
               x-model="denomForm.productNewName" :disabled="denomSaving">
        <input type="text" class="input input-bordered input-sm w-full" placeholder="Provider (opsional)"
               x-model="denomForm.productNewProvider" :disabled="denomSaving">
        <input type="text" class="input input-bordered input-sm w-full" placeholder="Icon URL (opsional)"
               x-model="denomForm.productNewIcon" :disabled="denomSaving">
      </div>
    </template>
  </div>
</div>
```

- [ ] **Step 3: Seed the combobox in `openCreateModal` / `openEditModal`**

```javascript
openCreateModal() {
    this.denomEditMode = false; this.denomEditId = null;
    this.denomForm = this.emptyDenomForm();
    // preselect current scope
    if (this.activeCategory) this.denomForm.categoryChoice = this.activeCategory;
    if (this.activeProduct)  this.denomForm.productChoice  = this.activeProduct;
    this.showDenomModal = true;
},
openEditModal(d) {
    this.denomEditMode = true; this.denomEditId = d.id;
    const prod = this.productById(d.productId);
    this.denomForm = { ...this.emptyDenomForm(),
        code: d.code, name: d.name, denomType: d.denomType, nominal: d.nominal, price: d.price,
        basePrice: d.basePrice, adminFee: d.adminFee, validityDays: d.validityDays, quotaMb: d.quotaMb,
        minAmount: d.minAmount, maxAmount: d.maxAmount, requiresInquiry: d.requiresInquiry,
        stockAvailable: d.stockAvailable, active: d.active, sortOrder: d.sortOrder,
        categoryChoice: prod ? prod.categoryId : '', productChoice: d.productId };
    this.showDenomModal = true;
},
```

Update `emptyDenomForm()` to include the new combobox fields (remove `productId`/`reassignCategory`).

- [ ] **Step 4: Rewrite `saveDenom` with orchestration**

```javascript
async resolveCategoryId() {
    if (!this.catComboIsNew) return this.denomForm.categoryChoice;
    const body = { code: this.slugCode(this.denomForm.categoryNewName), name: this.denomForm.categoryNewName.trim(),
                   categoryType: this.denomForm.categoryNewType, iconUrl: this.denomForm.categoryNewIcon, sortOrder: 0, active: true };
    const res = await fetch('/api/admin/catalog/categories', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(body) });
    if (!res.ok) throw new Error('Gagal membuat kategori');
    return (await res.json()).id;
},
async resolveProductId(categoryId) {
    if (!this.prodComboIsNew) return this.denomForm.productChoice;
    const body = { categoryId, code: this.slugCode(this.denomForm.productNewName), name: this.denomForm.productNewName.trim(),
                   providerName: this.denomForm.productNewProvider, description:'', iconUrl: this.denomForm.productNewIcon, sortOrder:0, active:true };
    const res = await fetch('/api/admin/catalog/products', { method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(body) });
    if (!res.ok) throw new Error('Gagal membuat produk');
    return (await res.json()).id;
},
async saveDenom() {
    this.denomSaving = true;
    try {
        const categoryId = await this.resolveCategoryId();
        const productId  = await this.resolveProductId(categoryId);
        if (!productId) throw new Error('Produk wajib dipilih');
        const payload = { code: this.denomForm.code, name: this.denomForm.name, denomType: this.denomForm.denomType,
            nominal: this.denomForm.nominal, price: this.denomForm.price, basePrice: this.denomForm.basePrice,
            adminFee: this.denomForm.adminFee, validityDays: this.denomForm.validityDays, quotaMb: this.denomForm.quotaMb,
            minAmount: this.denomForm.minAmount, maxAmount: this.denomForm.maxAmount, requiresInquiry: this.denomForm.requiresInquiry,
            stockAvailable: this.denomForm.stockAvailable, active: this.denomForm.active, sortOrder: this.denomForm.sortOrder,
            productId };
        const url = this.denomEditMode ? `/api/admin/catalog/denoms/${this.denomEditId}`
                                       : `/api/admin/catalog/products/${productId}/denoms`;
        const res = await fetch(url, { method: this.denomEditMode ? 'PUT':'POST',
            headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) });
        if (!res.ok) { const err = await res.json().catch(()=>({})); throw new Error(err.errorMessage || err.message || 'Gagal menyimpan'); }
        this.showDenomModal = false;
        await this.reloadCategories(); await this.reloadProducts(); await this.refreshDenoms();
        Alpine.store('toast').success(this.denomEditMode ? 'Denom diperbarui' : 'Denom ditambahkan');
    } catch (e) { Alpine.store('toast').error(e.message); }
    finally { this.denomSaving = false; }
},
```

`// ponytail: 3-step client orchestration (cat → product → denom); a mid-step failure leaves an orphan cat/product, surfaced via toast. Promote to one transactional endpoint only if orphans become a real problem.`

- [ ] **Step 5: Manual verification**

Restart app. `Tambah Denom` in Semua/Semua scope → in the modal type a new category (reveals Tipe + Icon), a new product (reveals Provider + Icon), fill denom, Simpan. Expected: category, product, denom created in one save; the new category+product appear as tabs (they now own a denom); toast success. Edit an existing denom → change its product via the combobox → denom moves.

- [ ] **Step 6: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/index.html
git commit -m "feat(catalog-ui): combobox denom modal creates category/product inline"
```

---

### Task 10: Tab-pill edit + remove products table & create buttons

**Files:**
- Modify: `index.html` — remove `+ Kategori`/`+ Produk` buttons; keep category/product modals as edit-only reached from the tab pencils; simplify search placeholder.

**Interfaces:**
- Consumes: existing `openCategoryModal(cat)`, `openProductModal(prod)`, delete flows.
- Produces: none.

> Note: the products-table panel was already removed in Task 7 Step 2 (denom table is the sole content). This task only handles the buttons, pencils, and search.

- [ ] **Step 1: Remove the create buttons; keep tab pencils (edit-only)**

Delete the `+ Kategori` button (`@click="openCategoryModal()"`) and the `+ Produk` button (`@click="openProductModal(...)"`). Keep the pencil buttons on the active category/product pills (they already call `openCategoryModal(c)` / `openProductModal(p)`), which open the existing modals for editing incl. their `Hapus`.

- [ ] **Step 2: Simplify search placeholder**

Change the search input placeholder binding to a constant:

```html
<input type="search" x-model="searchQuery" placeholder="Cari denominasi..." class="grow bg-transparent outline-none"/>
```

- [ ] **Step 3: Manual verification**

Restart app. Expected: no products table anywhere; no `+ Kategori`/`+ Produk` buttons; active category/product pill pencils still open the full edit modal (icon/type/provider/Hapus). Search always filters denoms. Create is only via `Tambah Denom` (Task 9).

- [ ] **Step 4: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/index.html
git commit -m "feat(catalog-ui): drop products table + create buttons; tab-pill edit only"
```

---

### Task 11: Sync DF — read-only preview + Terapkan Semua

**Files:**
- Modify: `index.html` — replace the per-product Sync Denom modal with the global Sync-All preview modal; move the `Sync DF` button to the toolbar (always visible).

**Interfaces:**
- Consumes: `GET /api/admin/catalog/sync/all/preview` (Task 5) → `{newCategories,newProducts,newDenoms,priceChanges}`; `POST /api/admin/catalog/sync/all`; `refreshDenoms()`, `reloadCategories()`, `reloadProducts()`.
- Produces: none.

- [ ] **Step 1: Replace sync state/methods**

Remove `denomSyncItems`, `syncGroup`, `toggleGroup`, `openSyncDenoms`, `confirmSync`. Add:

```javascript
showSyncModal: false,
syncing: false,
syncPreview: { newCategories: [], newProducts: [], newDenoms: [], priceChanges: [] },

async openSyncAll() {
    this.syncing = true;
    try {
        const res = await fetch('/api/admin/catalog/sync/all/preview');
        if (!res.ok) throw new Error();
        this.syncPreview = await res.json();
        this.showSyncModal = true;
    } catch (e) { Alpine.store('toast').error('Gagal ambil preview'); }
    finally { this.syncing = false; }
},
async applySyncAll() {
    this.syncing = true;
    try {
        const res = await fetch('/api/admin/catalog/sync/all', { method: 'POST' });
        if (!res.ok) throw new Error();
        const r = await res.json();
        Alpine.store('toast').success(`Selesai: +${r.added} baru, ${r.updated} update, ${r.failed} gagal`);
        this.showSyncModal = false;
        await this.reloadCategories(); await this.reloadProducts(); await this.refreshDenoms(); await this.loadSupplierPrices();
    } catch (e) { Alpine.store('toast').error('Gagal sync'); }
    finally { this.syncing = false; }
},
```

- [ ] **Step 2: Move the `Sync DF` button to the always-visible toolbar**

Put a single button in the page header row (near the `<h1>Katalog`), not inside the per-product denom header:

```html
<button sec:authorize="hasRole('REALM_manage_denoms')" x-show="canManageDenom"
        class="btn btn-outline btn-sm tap" @click="openSyncAll()" :disabled="syncing">
    <span x-show="syncing" class="loading loading-spinner loading-xs"></span> Sync DF
</button>
```

Keep the `Tambah Denom` button (Task 9) available; it no longer needs a selected product.

- [ ] **Step 3: Replace the sync modal markup**

Swap the old `#showDenomSyncModal` modal for a read-only summary:

```html
<div class="modal" :class="{ 'modal-open': showSyncModal }">
  <div class="modal-box max-w-2xl">
    <h3 class="font-bold text-lg">Sync Digiflazz</h3>
    <p class="text-sm text-base-content/60 mt-1">Preview perubahan. Terapkan semua = tambah item baru + update harga (tidak menghapus).</p>
    <template x-for="grp in [
        {t:'Kategori baru', k:'newCategories'}, {t:'Produk baru', k:'newProducts'},
        {t:'Denom baru', k:'newDenoms'}, {t:'Harga berubah', k:'priceChanges'}]" :key="grp.k">
      <div class="mt-4" x-show="syncPreview[grp.k] && syncPreview[grp.k].length">
        <div class="font-semibold text-sm" x-text="grp.t + ' (' + syncPreview[grp.k].length + ')'"></div>
        <div class="max-h-40 overflow-y-auto mt-1 text-sm text-base-content/70">
          <template x-for="(label, i) in syncPreview[grp.k]" :key="grp.k + i"><div x-text="label"></div></template>
        </div>
      </div>
    </template>
    <div x-show="!(syncPreview.newCategories.length || syncPreview.newProducts.length || syncPreview.newDenoms.length || syncPreview.priceChanges.length)"
         class="mt-4 text-sm text-base-content/60">Sudah sinkron — tidak ada perubahan.</div>
    <div class="modal-action">
      <button class="btn btn-ghost" @click="showSyncModal = false" :disabled="syncing">Batal</button>
      <button class="btn btn-primary" @click="applySyncAll()" :disabled="syncing">
        <span x-show="syncing" class="loading loading-spinner loading-xs"></span> Terapkan Semua
      </button>
    </div>
  </div>
</div>
```

- [ ] **Step 4: Manual verification**

Restart app. Click `Sync DF` in the header. Expected: modal lists grouped counts + labels (new categories/products/denoms + price changes), or "Sudah sinkron". `Terapkan Semua` runs `POST /sync/all`, toasts the result, and the table + tabs + Harga Suplier refresh. (Against a live DF this hits the real supplier; if DF creds are absent in dev, the preview call surfaces a toast error — acceptable, verify the button + modal wiring with mocked/seeded data.)

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/index.html
git commit -m "feat(catalog-ui): global Sync DF preview + apply-all"
```

---

## Phase D — Verification

### Task 12: Full slice test + manual smoke

- [ ] **Step 1: Run the catalog + supplier tests**

Run: `./mvnw -q -pl satset-core test -Dtest='com.satset.catalog.**,com.satset.supplier.**'`
Expected: PASS, no regressions.

- [ ] **Step 2: Manual smoke against the running app**

Load `http://localhost:8080/admin/catalog` and verify end to end:
- Semua/Semua → all denoms; Kategori A/Semua → category A denoms; A/B → product B denoms.
- Auto-prune: empty category/product absent from tabs.
- `Tambah Denom` combobox creates category+product+denom in one save; new tabs appear.
- Tab-pill pencil edits an existing category/product (icon/type/provider/Hapus).
- `Harga Suplier` populates with the date caption and the ▲/▼ flag when ≠ Harga Beli.
- `Sync DF` shows the read-only preview and applies.

- [ ] **Step 3: Update task tracking**

Mark the task done in `Tasks.md` and the Google Tasks list `Z184dEJwWFlUSG1GTkdIYQ`.

- [ ] **Step 4: Finish the branch**

Use `superpowers:finishing-a-development-branch` to decide merge/PR.

---

## Self-Review notes

- **Spec coverage:** aggregate `GET /denoms` (T1) + seed (T2); Harga Suplier map/date/flag (T3, T4, T7); Sync preview + apply-all (T5, T11); denom-centric table + columns (T6, T7); auto-prune tabs (T8); combobox inline create (T9); tab-pill edit + removals (T10). All spec sections mapped.
- **Ceilings recorded:** orphan-on-partial-create (T9), prune freshness (T8), snapshot single-cache (T3), sync payload note (spec risks).
- **Type consistency:** `fetchSnapshot()`/`PriceListSnapshot`, `supplierPrices()`/`SupplierPriceView`, `syncAllPreview()`/`SyncAllPreview`, `findAllForAdmin()` used identically across tasks.
