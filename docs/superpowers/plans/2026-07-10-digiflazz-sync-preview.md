# Digiflazz Sync Preview + Selective Apply Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Ganti flow sync one-click (langsung tulis semua) jadi **preview modal → pilih (checkbox) → confirm → apply selektif**, 3 level (kategori/produk/denom). Tambah grup **Hapus** (soft-delete item katalog yang hilang dari DF, checkbox default OFF).

**Architecture:** `CatalogSyncService` recompute diff fresh, expose `preview*` (dry-run, no write) + `apply*(List<String> selectedKeys)`. Client cuma kirim key terpilih; server yang tentuin action + harga. UI: DaisyUI modal per level.

**Tech Stack:** Spring Boot 4, Java 25, JUnit5+Mockito+AssertJ, Thymeleaf+Alpine+DaisyUI. Build `mvn -f satset-core/pom.xml`.

**Base commit:** eb027c2 (feature `feat/digiflazz-catalog-sync`).

## Global Constraints

- TDD ketat: test dulu → FAIL → implement → PASS → commit.
- `SyncResult` di-rename ke `(int added, int updated, int deleted, int skipped, int failed)`.
- "Hapus" = `softDelete` (soft-delete `deleted=true, active=false`) via existing `CategoryDomainService.softDelete(UUID)`, `ProductDomainService.softDelete(UUID)`, `DenomDomainService.softDelete(UUID)`.
- Server recompute preview saat apply; hanya terima daftar KEY dari client (jangan percaya harga client).
- Transaction slice akses catalog **hanya** lewat `*DomainService` (ModularityTest).
- Error: `log.error`, jgn expose `e.getMessage()`.
- Perintah test: `mvn -f satset-core/pom.xml -Dtest=<Class> test`.
- Commit trailer: `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`.

## File Structure

- **Create** `transaction/model/SyncAction.java`, `transaction/model/SyncPreviewItem.java`.
- **Modify** `transaction/service/SyncResult.java` (rename fields).
- **Rewrite** `transaction/service/CatalogSyncService.java` (preview* + apply*, drop 3 all-apply methods).
- **Rewrite** `transaction/web/CatalogSyncController.java` (preview GET + apply POST body).
- **Modify** tests `CatalogSyncServiceTest`, `CatalogSyncControllerTest`.
- **Modify** templates `categories.html`, `products.html`, `denoms.html` (preview modal).

---

## Task 1: Model + CatalogSyncService (preview + apply selektif)

**Files:**
- Create: `satset-core/src/main/java/com/satset/transaction/model/SyncAction.java`, `.../SyncPreviewItem.java`
- Modify: `satset-core/src/main/java/com/satset/transaction/service/SyncResult.java`
- Rewrite: `satset-core/src/main/java/com/satset/transaction/service/CatalogSyncService.java`
- Test: `satset-core/src/test/java/com/satset/transaction/service/CatalogSyncServiceTest.java`

**Interfaces produced:**
- `enum SyncAction { ADD, UPDATE, DELETE }`
- `record SyncPreviewItem(SyncAction action, String key, String label, String detail)`
- `record SyncResult(int added, int updated, int deleted, int skipped, int failed)`
- `List<SyncPreviewItem> previewCategories()`; `SyncResult applyCategories(List<String> keys)`
- `List<SyncPreviewItem> previewProducts(UUID categoryId)`; `SyncResult applyProducts(UUID categoryId, List<String> keys)`
- `SyncResult applyDenoms(UUID productId, List<String> selectedSkus)`
- `List<PriceCompareRow> reconcileForProduct(UUID productId)` (unchanged)

**Existing catalog methods used:** `CategoryDomainService.{findAllForAdmin, findById, findOrCreateByName, softDelete}`, `ProductDomainService.{findByCategoryForAdmin, findById, findByCode, findOrCreateByBrand, softDelete}`, `DenomDomainService.{findActiveByProductId, createFromSupplier, updateCostById, softDelete}`, `CatalogCodeUtil.toCode`.

- [ ] **Step 1: Rewrite the test** `CatalogSyncServiceTest` (replace file). Keep the existing `@Mock digiflazz/categoryService/productService/denomService` + `df(...)`/`denom(...)` helpers; replace the old sync* tests with the following (add imports as needed: `com.satset.transaction.model.SyncAction`, `com.satset.transaction.model.SyncPreviewItem`, `com.satset.catalog.model.Category`, `com.satset.catalog.model.Products`):

```java
// ---- previewCategories ----
@Test void previewCategories_addsMissing_deletesNotInDf() {
    Category existing = new Category(); existing.setId(UUID.randomUUID()); existing.setCode("PULSA"); existing.setName("Pulsa");
    Category orphan = new Category(); orphan.setId(UUID.randomUUID()); orphan.setCode("GAME"); orphan.setName("Game Lama");
    when(categoryService.findAllForAdmin()).thenReturn(List.of(existing, orphan));
    when(digiflazz.fetchPriceList()).thenReturn(List.of(
            df("a","A","XL",1),                          // category "Pulsa" -> PULSA (exists)
            dfCat("b","B","E-Money","DANA",2)));          // category "E-Money" -> EMONEY (new)
    List<SyncPreviewItem> items = service().previewCategories();
    assertThat(items).anySatisfy(i -> { assertThat(i.action()).isEqualTo(SyncAction.ADD); assertThat(i.key()).isEqualTo("E-Money"); });
    assertThat(items).anySatisfy(i -> { assertThat(i.action()).isEqualTo(SyncAction.DELETE); assertThat(i.key()).isEqualTo(orphan.getId().toString()); });
    assertThat(items).noneMatch(i -> "Pulsa".equals(i.key()));
}

@Test void applyCategories_appliesOnlySelected() {
    Category orphan = new Category(); orphan.setId(UUID.randomUUID()); orphan.setCode("GAME"); orphan.setName("Game Lama");
    when(categoryService.findAllForAdmin()).thenReturn(List.of(orphan));
    when(digiflazz.fetchPriceList()).thenReturn(List.of(dfCat("b","B","E-Money","DANA",2)));
    // pilih hanya ADD "E-Money", TIDAK pilih DELETE orphan
    SyncResult r = service().applyCategories(List.of("E-Money"));
    verify(categoryService).findOrCreateByName("E-Money");
    verify(categoryService, never()).softDelete(any());
    assertThat(r.added()).isEqualTo(1);
    assertThat(r.deleted()).isZero();
}

// ---- previewProducts ----
@Test void previewProducts_addsMissingBrand_deletesOrphan() {
    UUID catId = UUID.randomUUID();
    Category cat = new Category(); cat.setId(catId); cat.setCode("PULSA");
    when(categoryService.findById(catId)).thenReturn(Optional.of(cat));
    Products orphan = new Products(); orphan.setId(UUID.randomUUID()); orphan.setCode("OLDBRAND"); orphan.setName("Old");
    when(productService.findByCategoryForAdmin(catId)).thenReturn(List.of(orphan));
    when(productService.findByCode("XL")).thenReturn(Optional.empty());
    when(digiflazz.fetchPriceList()).thenReturn(List.of(df("a","A","XL",1)));   // category "Pulsa" -> PULSA
    List<SyncPreviewItem> items = service().previewProducts(catId);
    assertThat(items).anySatisfy(i -> { assertThat(i.action()).isEqualTo(SyncAction.ADD); assertThat(i.key()).isEqualTo("XL"); });
    assertThat(items).anySatisfy(i -> { assertThat(i.action()).isEqualTo(SyncAction.DELETE); assertThat(i.key()).isEqualTo(orphan.getId().toString()); });
}

@Test void applyProducts_appliesOnlySelected() {
    UUID catId = UUID.randomUUID();
    Category cat = new Category(); cat.setId(catId); cat.setCode("PULSA");
    when(categoryService.findById(catId)).thenReturn(Optional.of(cat));
    when(productService.findByCategoryForAdmin(catId)).thenReturn(List.of());
    when(productService.findByCode("XL")).thenReturn(Optional.empty());
    when(digiflazz.fetchPriceList()).thenReturn(List.of(df("a","A","XL",1)));
    SyncResult r = service().applyProducts(catId, List.of("XL"));
    verify(productService).findOrCreateByBrand("XL", catId);
    assertThat(r.added()).isEqualTo(1);
}

// ---- applyDenoms (delete = softDelete) ----
@Test void applyDenoms_appliesSelected_hilangUsesSoftDelete() {
    UUID pid = UUID.randomUUID();
    Products p = new Products(); p.setId(pid); p.setCode("XL");
    when(productService.findById(pid)).thenReturn(Optional.of(p));
    when(digiflazz.fetchPriceList()).thenReturn(List.of(
            df("x5","XL 5","XL",5500)));                  // BARU (sku x5)
    UUID dOld = UUID.randomUUID();
    ProductDenoms old = denom("XOLD", new BigDecimal("1000")); old.setId(dOld);
    when(denomService.findActiveByProductId(pid)).thenReturn(List.of(old));  // XOLD -> HILANG
    // pilih BARU x5 dan HILANG XOLD (key HILANG = denom.code "XOLD")
    SyncResult r = service().applyDenoms(pid, List.of("x5", "XOLD"));
    verify(denomService).createFromSupplier(pid, "x5", "XL 5", new BigDecimal("5500"));
    verify(denomService).softDelete(dOld);
    assertThat(r.added()).isEqualTo(1);
    assertThat(r.deleted()).isEqualTo(1);
}

@Test void applyDenoms_unselected_skipped() {
    UUID pid = UUID.randomUUID();
    Products p = new Products(); p.setId(pid); p.setCode("XL");
    when(productService.findById(pid)).thenReturn(Optional.of(p));
    when(digiflazz.fetchPriceList()).thenReturn(List.of(df("x5","XL 5","XL",5500)));
    when(denomService.findActiveByProductId(pid)).thenReturn(List.of());
    SyncResult r = service().applyDenoms(pid, List.of());  // pilih kosong
    verify(denomService, never()).createFromSupplier(any(), any(), any(), any());
    assertThat(r.added()).isZero();
}
```

Update the existing `df(...)` helper to accept a category and add a `dfCat(...)` helper (keep old `df` for brand-only with default category "Pulsa"):

```java
private static PriceListItem df(String sku, String name, String brand, long price) {
    return dfCat(sku, name, "Pulsa", brand, price);
}
private static PriceListItem dfCat(String sku, String name, String category, String brand, long price) {
    return new PriceListItem(name, category, brand, "Umum", sku, price, true, true, false, "0", "Ki***", "");
}
```
Delete the old tests that referenced removed methods `syncCategories()/syncProducts(UUID)/syncDenoms(UUID)` and the old `SyncResult` field names (`created/costUpdated/deactivated`). Keep `reconcileForProduct_*` tests (unchanged) but update any `SyncResult` field references.

- [ ] **Step 2: Run — FAIL** `mvn -f satset-core/pom.xml -Dtest=CatalogSyncServiceTest test` (compile errors: new types/methods missing).

- [ ] **Step 3: Create SyncAction**

```java
package com.satset.transaction.model;
/** Aksi sync katalog vs Digiflazz. */
public enum SyncAction { ADD, UPDATE, DELETE }
```

- [ ] **Step 4: Create SyncPreviewItem**

```java
package com.satset.transaction.model;
/** Satu item preview sync. key = identitas buat apply (nama/brand/sku utk ADD, UUID utk DELETE, sku denom utk UPDATE). */
public record SyncPreviewItem(SyncAction action, String key, String label, String detail) {}
```

- [ ] **Step 5: Rename SyncResult fields** — replace `transaction/service/SyncResult.java`:

```java
package com.satset.transaction.service;
/** Ringkasan hasil apply sync katalog. */
public record SyncResult(int added, int updated, int deleted, int skipped, int failed) {}
```

- [ ] **Step 6: Rewrite CatalogSyncService** — replace the three all-apply methods (`syncCategories()`, `syncProducts(UUID)`, `syncDenoms(UUID)`) with preview+apply. Keep constructor, `reconcileForProduct` unchanged. New imports: `com.satset.transaction.model.SyncAction`, `com.satset.transaction.model.SyncPreviewItem`. Full new method set:

```java
// ===== Categories =====
public List<SyncPreviewItem> previewCategories() {
    List<Category> catalog = categoryService.findAllForAdmin();
    Set<String> catalogCodes = catalog.stream().filter(c -> !c.isDeleted())
            .map(Category::getCode).collect(Collectors.toSet());
    List<SyncPreviewItem> items = new ArrayList<>();
    Set<String> dfCodes = new HashSet<>();
    Set<String> seen = new HashSet<>();
    for (PriceListItem it : digiflazz.fetchPriceList()) {
        String code = CatalogCodeUtil.toCode(it.category());
        dfCodes.add(code);
        if (seen.add(code) && !catalogCodes.contains(code)) {
            items.add(new SyncPreviewItem(SyncAction.ADD, it.category(), it.category(), null));
        }
    }
    for (Category c : catalog) {
        if (!c.isDeleted() && !dfCodes.contains(c.getCode())) {
            items.add(new SyncPreviewItem(SyncAction.DELETE, c.getId().toString(), c.getName(), "hilang dari DF"));
        }
    }
    return items;
}

public SyncResult applyCategories(List<String> keys) {
    Set<String> sel = new HashSet<>(keys);
    int added = 0, deleted = 0, skipped = 0, failed = 0;
    for (SyncPreviewItem it : previewCategories()) {
        if (!sel.contains(it.key())) { skipped++; continue; }
        try {
            switch (it.action()) {
                case ADD -> { categoryService.findOrCreateByName(it.key()); added++; }
                case DELETE -> { categoryService.softDelete(UUID.fromString(it.key())); deleted++; }
                default -> skipped++;
            }
        } catch (Exception e) { log.error("applyCategories gagal utk {}", it.label(), e); failed++; }
    }
    return new SyncResult(added, 0, deleted, skipped, failed);
}

// ===== Products =====
public List<SyncPreviewItem> previewProducts(UUID categoryId) {
    Category cat = categoryService.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    List<Products> catalog = productService.findByCategoryForAdmin(categoryId);
    List<SyncPreviewItem> items = new ArrayList<>();
    Set<String> dfBrandCodes = new HashSet<>();
    Set<String> seen = new HashSet<>();
    for (PriceListItem it : digiflazz.fetchPriceList()) {
        if (!CatalogCodeUtil.toCode(it.category()).equals(cat.getCode())) continue;
        String code = CatalogCodeUtil.toCode(it.brand());
        dfBrandCodes.add(code);
        if (seen.add(code) && productService.findByCode(code).isEmpty()) {
            items.add(new SyncPreviewItem(SyncAction.ADD, it.brand(), it.brand(), null));
        }
    }
    for (Products p : catalog) {
        if (!p.isDeleted() && !dfBrandCodes.contains(p.getCode())) {
            items.add(new SyncPreviewItem(SyncAction.DELETE, p.getId().toString(), p.getName(), "hilang dari DF"));
        }
    }
    return items;
}

public SyncResult applyProducts(UUID categoryId, List<String> keys) {
    Set<String> sel = new HashSet<>(keys);
    int added = 0, deleted = 0, skipped = 0, failed = 0;
    for (SyncPreviewItem it : previewProducts(categoryId)) {
        if (!sel.contains(it.key())) { skipped++; continue; }
        try {
            switch (it.action()) {
                case ADD -> { productService.findOrCreateByBrand(it.key(), categoryId); added++; }
                case DELETE -> { productService.softDelete(UUID.fromString(it.key())); deleted++; }
                default -> skipped++;
            }
        } catch (Exception e) { log.error("applyProducts gagal utk {}", it.label(), e); failed++; }
    }
    return new SyncResult(added, 0, deleted, skipped, failed);
}

// ===== Denoms (preview = reconcileForProduct; UI map status->action) =====
public SyncResult applyDenoms(UUID productId, List<String> selectedSkus) {
    Set<String> sel = new HashSet<>(selectedSkus);
    int added = 0, updated = 0, deleted = 0, skipped = 0, failed = 0;
    for (PriceCompareRow r : reconcileForProduct(productId)) {
        if (r.status() == CompareStatus.SAMA || !sel.contains(r.buyerSku())) { skipped++; continue; }
        try {
            switch (r.status()) {
                case BARU -> { denomService.createFromSupplier(productId, r.buyerSku(), r.productName(), r.dfCost()); added++; }
                case NAIK, TURUN -> { denomService.updateCostById(r.denomId(), r.dfCost()); updated++; }
                case HILANG -> { denomService.softDelete(r.denomId()); deleted++; }
                case SAMA -> skipped++;
            }
        } catch (Exception e) { log.error("applyDenoms gagal utk SKU {}", r.buyerSku(), e); failed++; }
    }
    return new SyncResult(added, updated, deleted, skipped, failed);
}
```

- [ ] **Step 7: Run — PASS** `mvn -f satset-core/pom.xml -Dtest=CatalogSyncServiceTest test`.

- [ ] **Step 8: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/model/SyncAction.java \
        satset-core/src/main/java/com/satset/transaction/model/SyncPreviewItem.java \
        satset-core/src/main/java/com/satset/transaction/service/SyncResult.java \
        satset-core/src/main/java/com/satset/transaction/service/CatalogSyncService.java \
        satset-core/src/test/java/com/satset/transaction/service/CatalogSyncServiceTest.java
git commit -m "feat(pricelist): preview + selective apply + delete offer (service)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: CatalogSyncController — preview GET + apply POST(body)

**Files:** Rewrite `transaction/web/CatalogSyncController.java`; Modify `transaction/web/CatalogSyncControllerTest.java`.

**Interfaces:** preview GET (categories, products) + apply POST with `@RequestBody List<String>`; existing `GET /products/{id}/pricelist-compare` kept.

- [ ] **Step 1: Rewrite the test** `CatalogSyncControllerTest` (adapt to new `SyncResult(added,updated,deleted,skipped,failed)` + new endpoints):

```java
package com.satset.transaction.web;

import com.satset.transaction.model.SyncAction;
import com.satset.transaction.model.SyncPreviewItem;
import com.satset.transaction.service.CatalogSyncService;
import com.satset.transaction.service.SyncResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CatalogSyncControllerTest {

    @Mock CatalogSyncService sync;
    private MockMvc mockMvc() { return MockMvcBuilders.standaloneSetup(new CatalogSyncController(sync)).build(); }

    @Test void previewCategories_returnsItems() throws Exception {
        when(sync.previewCategories()).thenReturn(List.of(new SyncPreviewItem(SyncAction.ADD, "E-Money", "E-Money", null)));
        mockMvc().perform(get("/api/admin/catalog/sync/categories/preview"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].key").value("E-Money"));
    }
    @Test void applyCategories_passesSelectedKeys() throws Exception {
        when(sync.applyCategories(List.of("E-Money"))).thenReturn(new SyncResult(1,0,0,0,0));
        mockMvc().perform(post("/api/admin/catalog/sync/categories")
                        .contentType(MediaType.APPLICATION_JSON).content("[\"E-Money\"]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.added").value(1));
        verify(sync).applyCategories(List.of("E-Money"));
    }
    @Test void previewProducts_passesCategoryId() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.previewProducts(id)).thenReturn(List.of());
        mockMvc().perform(get("/api/admin/catalog/categories/" + id + "/sync/products/preview"))
                .andExpect(status().isOk());
    }
    @Test void applyProducts_passesIdAndKeys() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.applyProducts(eq(id), eq(List.of("XL")))).thenReturn(new SyncResult(1,0,0,0,0));
        mockMvc().perform(post("/api/admin/catalog/categories/" + id + "/sync/products")
                        .contentType(MediaType.APPLICATION_JSON).content("[\"XL\"]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.added").value(1));
    }
    @Test void applyDenoms_passesIdAndSkus() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.applyDenoms(eq(id), eq(List.of("x5")))).thenReturn(new SyncResult(1,0,1,0,0));
        mockMvc().perform(post("/api/admin/catalog/products/" + id + "/sync/denoms")
                        .contentType(MediaType.APPLICATION_JSON).content("[\"x5\"]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(1));
    }
    @Test void compare_returnsRows() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.reconcileForProduct(id)).thenReturn(List.of());
        mockMvc().perform(get("/api/admin/catalog/products/" + id + "/pricelist-compare"))
                .andExpect(status().isOk());
        verify(sync).reconcileForProduct(id);
    }
}
```

- [ ] **Step 2: Run — FAIL** (`-Dtest=CatalogSyncControllerTest`).

- [ ] **Step 3: Rewrite controller**

```java
package com.satset.transaction.web;

import com.satset.shared.constant.SatsetConstants;
import com.satset.transaction.model.PriceCompareRow;
import com.satset.transaction.model.SyncPreviewItem;
import com.satset.transaction.service.CatalogSyncService;
import com.satset.transaction.service.SyncResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Endpoint preview + apply sync katalog per-level dgn Digiflazz. */
@RestController
@RequestMapping("/api/admin/catalog")
public class CatalogSyncController {

    private final CatalogSyncService sync;
    public CatalogSyncController(CatalogSyncService sync) { this.sync = sync; }

    // Categories
    @GetMapping("/sync/categories/preview")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public List<SyncPreviewItem> previewCategories() { return sync.previewCategories(); }

    @PostMapping("/sync/categories")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public SyncResult applyCategories(@RequestBody List<String> keys) { return sync.applyCategories(keys); }

    // Products
    @GetMapping("/categories/{categoryId}/sync/products/preview")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public List<SyncPreviewItem> previewProducts(@PathVariable UUID categoryId) { return sync.previewProducts(categoryId); }

    @PostMapping("/categories/{categoryId}/sync/products")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public SyncResult applyProducts(@PathVariable UUID categoryId, @RequestBody List<String> keys) {
        return sync.applyProducts(categoryId, keys);
    }

    // Denoms (preview = compare)
    @GetMapping("/products/{productId}/pricelist-compare")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public List<PriceCompareRow> compare(@PathVariable UUID productId) { return sync.reconcileForProduct(productId); }

    @PostMapping("/products/{productId}/sync/denoms")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public SyncResult applyDenoms(@PathVariable UUID productId, @RequestBody List<String> selectedSkus) {
        return sync.applyDenoms(productId, selectedSkus);
    }
}
```

- [ ] **Step 4: Run — PASS**.
- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/web/CatalogSyncController.java \
        satset-core/src/test/java/com/satset/transaction/web/CatalogSyncControllerTest.java
git commit -m "feat(pricelist): preview GET + apply POST(selected keys) endpoints

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: UI preview modal (categories/products/denoms)

**Files:** Modify `categories.html`, `products.html`, `denoms.html` (all under `resources/templates/pages/admin/catalog/`). No unit test (manual verify Task 4).

**Shared pattern** — each page's Sync button opens a modal built from preview data. Modal groups items by action (Tambah/Update/Hapus), each item a labeled checkbox; a "pilih semua" toggle per group; **Hapus group checkboxes default UNCHECKED**, Tambah/Update default checked. Confirm collects checked keys → POST → toast → reload. Reuse `Alpine.store('toast')`. CSRF auto (base.html). Follow existing `*Manager()` object + DaisyUI `modal`/`modal-open` conventions already in these files (they already use modals for create/edit).

Reusable modal state shape to add into each manager return object:
```javascript
syncing: false,
showSyncModal: false,
syncItems: [],          // [{action,key,label,detail,checked}]
syncTitle: '',
async openSync(previewUrl, title) {
    this.syncing = true; this.syncTitle = title;
    try {
        const res = await fetch(previewUrl);
        if (!res.ok) throw new Error('gagal');
        const raw = await res.json();
        this.syncItems = raw.map(i => ({ ...i, checked: i.action !== 'DELETE' })); // DELETE default off
        if (this.syncItems.length === 0) { Alpine.store('toast').success('Sudah sinkron dengan Digiflazz'); return; }
        this.showSyncModal = true;
    } catch (e) { Alpine.store('toast').error('Gagal ambil preview'); }
    finally { this.syncing = false; }
},
syncGroup(action) { return this.syncItems.filter(i => i.action === action); },
toggleGroup(action, val) { this.syncItems.forEach(i => { if (i.action === action) i.checked = val; }); },
async confirmSync(applyUrl, reload) {
    const keys = this.syncItems.filter(i => i.checked).map(i => i.key);
    this.syncing = true;
    try {
        const res = await fetch(applyUrl, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(keys) });
        if (!res.ok) throw new Error('gagal');
        const r = await res.json();
        Alpine.store('toast').success(`Selesai: +${r.added} baru, ${r.updated} update, ${r.deleted} hapus, ${r.failed} gagal`);
        this.showSyncModal = false;
        await reload();
    } catch (e) { Alpine.store('toast').error('Gagal sync'); }
    finally { this.syncing = false; }
},
```

Reusable modal markup (place inside the page's content fragment; adjust group labels; for denoms the UPDATE group is relevant, for categories/products only ADD+DELETE appear):
```html
<div class="modal" :class="{ 'modal-open': showSyncModal }">
  <div class="modal-box max-w-2xl">
    <h3 class="font-bold text-lg" x-text="syncTitle">Preview Sync</h3>
    <template x-for="grp in ['ADD','UPDATE','DELETE']" :key="grp">
      <div x-show="syncGroup(grp).length" class="mt-4">
        <div class="flex items-center justify-between">
          <span class="font-semibold text-sm"
                x-text="grp === 'ADD' ? '➕ Tambah' : grp === 'UPDATE' ? '✏️ Update harga' : '🗑️ Hapus'"></span>
          <label class="label cursor-pointer gap-2 text-xs">
            <span>pilih semua</span>
            <input type="checkbox" class="checkbox checkbox-xs"
                   :checked="syncGroup(grp).every(i => i.checked)"
                   @change="toggleGroup(grp, $event.target.checked)">
          </label>
        </div>
        <div class="max-h-48 overflow-y-auto mt-1">
          <template x-for="it in syncGroup(grp)" :key="it.key">
            <label class="flex items-center gap-2 py-1 text-sm cursor-pointer">
              <input type="checkbox" class="checkbox checkbox-sm" x-model="it.checked">
              <span x-text="it.label"></span>
              <span class="text-xs text-base-content/50" x-show="it.detail" x-text="it.detail"></span>
            </label>
          </template>
        </div>
      </div>
    </template>
    <div class="modal-action">
      <button class="btn btn-ghost" @click="showSyncModal = false" :disabled="syncing">Batal</button>
      <button class="btn btn-primary" @click="CONFIRM_CALL" :disabled="syncing">
        <span x-show="syncing" class="loading loading-spinner loading-xs"></span> Terapkan
      </button>
    </div>
  </div>
</div>
```

- [ ] **Step 1: categories.html** — replace the existing `syncCategories()` method + button `@click` so the button calls `openSync('/api/admin/catalog/sync/categories/preview', 'Sync Kategori')` and add the modal markup with confirm button `@click="confirmSync('/api/admin/catalog/sync/categories', () => loadCategories())"`. Remove the old confirm-dialog `syncCategories()` body (replaced by openSync/confirmSync). Add the shared state/methods above into `categoryManager()`.

- [ ] **Step 2: products.html** — button `@click="openSync('/api/admin/catalog/categories/' + filterCategoryId + '/sync/products/preview', 'Sync Produk')"` (keep `:disabled="!filterCategoryId"`), modal confirm `@click="confirmSync('/api/admin/catalog/categories/' + filterCategoryId + '/sync/products', () => loadProducts())"`. Add shared state/methods into `productManager()`; remove old `syncProducts()` one-click body.

- [ ] **Step 3: denoms.html** — denom preview comes from the compare endpoint (returns rows with `status`, not SyncPreviewItem). In `denomManager()`, add a denom-specific `openSyncDenoms()` that fetches `/api/admin/catalog/products/${productId}/pricelist-compare`, maps rows to items:
```javascript
async openSyncDenoms() {
    this.syncing = true; this.syncTitle = 'Sync Denom';
    try {
        const res = await fetch(`/api/admin/catalog/products/${this.productId}/pricelist-compare`);
        if (!res.ok) throw new Error('gagal');
        const rows = await res.json();
        const map = { BARU: 'ADD', NAIK: 'UPDATE', TURUN: 'UPDATE', HILANG: 'DELETE' };
        this.syncItems = rows.filter(r => r.status !== 'SAMA').map(r => ({
            action: map[r.status], key: r.buyerSku, label: r.productName || r.buyerSku,
            detail: r.status === 'HILANG' ? 'hilang dari DF'
                  : (r.dfCost != null ? 'Rp ' + new Intl.NumberFormat('id-ID').format(r.dfCost) : ''),
            checked: r.status !== 'HILANG'   // DELETE default off
        }));
        if (this.syncItems.length === 0) { Alpine.store('toast').success('Denom sudah sinkron'); return; }
        this.showSyncModal = true;
    } catch (e) { Alpine.store('toast').error('Gagal ambil preview'); }
    finally { this.syncing = false; }
},
```
Button `@click="openSyncDenoms()"`; modal confirm `@click="confirmSync('/api/admin/catalog/products/' + productId + '/sync/denoms', async () => { await loadDenoms(); await loadCompare(); })"`. Reuse the shared `syncGroup/toggleGroup/confirmSync/showSyncModal/syncItems/syncTitle/syncing` state. Keep the existing "Harga DF" column + `loadCompare()`.

- [ ] **Step 4: Verify compile + syntax** `mvn -f satset-core/pom.xml -q -DskipTests compile` (BUILD SUCCESS). Re-read each diff: Alpine objects valid (commas/braces), Thymeleaf/`x-for`/`x-model` well-formed, `CONFIRM_CALL` placeholder replaced with the real per-page confirm call.

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/categories.html \
        satset-core/src/main/resources/templates/pages/admin/catalog/products.html \
        satset-core/src/main/resources/templates/pages/admin/catalog/denoms.html
git commit -m "feat(pricelist): preview modal + checkbox selective apply + delete offer (UI)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Regression + E2E

- [ ] **Step 1: Full affected test run**

```bash
mvn -f satset-core/pom.xml -Dtest='DigiflazzClientTest,CatalogCodeUtilTest,CategoryDomainServiceTest,ProductDomainServiceTest,DenomDomainServiceTest,CatalogSyncServiceTest,CatalogSyncControllerTest,ModularityTest' test
```
Expected: BUILD SUCCESS, all green.

- [ ] **Step 2: Boot + manual E2E** — `.env` ada DIGIFLAZZ_* + PROXY_PASS. Login admin.
  - Categories page → "Sync Kategori" → modal muncul (grup Tambah checked, Hapus unchecked) → uncentang beberapa → Terapkan → toast; cek DB.
  - Products page (pilih kategori) → "Sync Produk" → modal → Terapkan.
  - Denom page → "Sync Denom" → modal (Tambah/Update checked, Hapus off) → Terapkan; cek denom `code` lowercase, HILANG yang dicentang → `deleted=t, active=f`.
  - Klik sync saat sudah sinkron → toast "sudah sinkron" (modal tak muncul).

- [ ] **Step 3: graphify + tasks**

```bash
graphify update .
```

- [ ] **Step 4: Commit sisa jika ada.**

## Self-Review Notes
- Spec coverage: preview dry-run + selective apply + delete offer (soft-delete) + check-all + DELETE-default-off → Task 1 (service), Task 2 (endpoints), Task 3 (UI). Server recompute on apply → apply* re-runs preview*.
- Type consistency: `SyncResult(added,updated,deleted,skipped,failed)`, `SyncPreviewItem(action,key,label,detail)`, `SyncAction{ADD,UPDATE,DELETE}` used consistently across Task 1/2/3.
- Boundary: apply DELETE uses `*DomainService.softDelete` (catalog service), not repository.
- Caveat: manual catalog items appear in DELETE group (default off, user decides). Apply per-item, not atomic. No undo post-confirm.
