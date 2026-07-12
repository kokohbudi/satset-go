# Digiflazz Catalog Sync (per-level) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tombol sync Digiflazz **per-level di halaman katalog**: Kategori→create-missing kategori, Produk→create-missing brand (dalam kategori dibuka), Denom→full-mirror SKU brand itu (BARU/NAIK/TURUN/HILANG) + kolom delta harga.

**Architecture:** `CatalogSyncService` (transaction slice) inject `DigiflazzClient` + 3 catalog `*DomainService`; recompute dari DF fresh per aksi, apply lewat service catalog (write tetap di catalog → ModularityTest aman). REST `CatalogSyncController` (transaction/web). UI = tombol di `categories.html`/`products.html`/`denoms.html` + `fetch` (CSRF auto). Halaman standalone `/admin/pricelist` **dibongkar**.

**Tech Stack:** Spring Boot 4, Java 25, JPA, JUnit5+Mockito+AssertJ, Thymeleaf+Alpine+DaisyUI. Build `mvn -f satset-core/pom.xml` (wrapper `./mvnw` di-rewrite ke `mvn` oleh rtk).

## Global Constraints

- TDD ketat: test dulu → run FAIL → implement minimal → run PASS → commit.
- Denom hasil-sync: `code` disimpan **apa adanya** (lowercase DF), TIDAK di-uppercase.
- Category.code = `toCode(DF category)`, Product.code = `toCode(DF brand)`, Denom.code = `buyer_sku_code`.
- Transaction slice akses catalog **hanya** lewat `*DomainService` (bukan repository) — ModularityTest.
- Error: `log.error(...)`, JANGAN expose `e.getMessage()` ke client.
- Perintah test: `mvn -f satset-core/pom.xml -Dtest=<Kelas> test`.
- Commit trailer: `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`.
- File pricelist lama (`AdminPriceListPageController*`, `pricelist.html`, `PriceReconcileService*`) **belum di-commit** (untracked) → hapus pakai `rm`/Write-kosong, bukan `git rm`.

## File Structure

- **Modify** `transaction/model/PriceListItem.java` — `sellerName`, `stock` int→String.
- **Modify** `transaction/model/PriceCompareRow.java` — tambah `seller`, `denomId`.
- **Delete** `transaction/web/AdminPriceListPageController.java`, `transaction/service/PriceReconcileService.java`, `resources/templates/pages/admin/pricelist.html`, test `AdminPriceListPageControllerTest`, `PriceReconcileServiceTest`.
- **Modify** `catalog/service/DenomDomainService.java` — buang `findAllActive`; tambah `findActiveByProductId`, `createFromSupplier`, `updateCostById`, `deactivateById`.
- **Modify** `catalog/repository/DenomRepository.java` — buang `findByActiveTrueAndDeletedFalse`.
- **Create** `catalog/service/CatalogCodeUtil.java`.
- **Modify** `catalog/service/CategoryDomainService.java` — `findOrCreateByName`.
- **Modify** `catalog/service/ProductDomainService.java` — `findOrCreateByBrand`.
- **Create** `transaction/service/SyncResult.java`, `transaction/service/CatalogSyncService.java`.
- **Create** `transaction/web/CatalogSyncController.java`.
- **Modify** templates `categories.html`, `products.html`, `denoms.html`.

---

## Task 1: PriceListItem — sellerName + stock String

**Files:** Modify `transaction/model/PriceListItem.java`; Test `transaction/client/DigiflazzClientTest.java`.

**Interfaces:** Produces `record PriceListItem(String productName, String category, String brand, String type, String buyerSkuCode, long price, boolean buyerProductStatus, boolean sellerProductStatus, boolean unlimitedStock, String stock, String sellerName, String desc)`.

- [ ] **Step 1: Update test** — di `DigiflazzClientTest`, item JSON: `stock` jadi `"100"` (string), tambah `"seller_name":"Ki***"`. Ganti assert `it.stock()`:

```java
assertThat(it.stock()).isEqualTo("100");
assertThat(it.sellerName()).isEqualTo("Ki***");
```

- [ ] **Step 2: Run — FAIL** `mvn -f satset-core/pom.xml -Dtest=DigiflazzClientTest test` (kompilasi: `stock()` int, `sellerName()` absen).

- [ ] **Step 3: Modify record**

```java
public record PriceListItem(
        @JsonProperty("product_name") String productName,
        String category, String brand, String type,
        @JsonProperty("buyer_sku_code") String buyerSkuCode,
        long price,
        @JsonProperty("buyer_product_status") boolean buyerProductStatus,
        @JsonProperty("seller_product_status") boolean sellerProductStatus,
        @JsonProperty("unlimited_stock") boolean unlimitedStock,
        String stock,
        @JsonProperty("seller_name") String sellerName,
        String desc) {
}
```

- [ ] **Step 4: Run — PASS**.
- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/model/PriceListItem.java \
        satset-core/src/test/java/com/satset/transaction/client/DigiflazzClientTest.java
git commit -m "feat(pricelist): PriceListItem sellerName + stock String

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Bongkar halaman /admin/pricelist + siapkan PriceCompareRow

Menghapus halaman standalone (diganti sync per-level) dan menyiapkan `PriceCompareRow` dengan `seller`+`denomId` untuk dipakai `reconcileForProduct` (Task 5).

**Files:**
- Delete: `transaction/web/AdminPriceListPageController.java`, `transaction/service/PriceReconcileService.java`, `resources/templates/pages/admin/pricelist.html`, `transaction/web/AdminPriceListPageControllerTest.java`, `transaction/service/PriceReconcileServiceTest.java`.
- Modify: `transaction/model/PriceCompareRow.java`; `catalog/service/DenomDomainService.java` (buang `findAllActive`); `catalog/repository/DenomRepository.java` (buang `findByActiveTrueAndDeletedFalse`).

**Interfaces:** Produces `record PriceCompareRow(String buyerSku, String productName, String brand, String category, String seller, BigDecimal dbCost, BigDecimal dfCost, BigDecimal delta, java.util.UUID denomId, CompareStatus status)`.

- [ ] **Step 1: Hapus file pricelist lama**

```bash
rm satset-core/src/main/java/com/satset/transaction/web/AdminPriceListPageController.java \
   satset-core/src/main/java/com/satset/transaction/service/PriceReconcileService.java \
   satset-core/src/main/resources/templates/pages/admin/pricelist.html \
   satset-core/src/test/java/com/satset/transaction/web/AdminPriceListPageControllerTest.java \
   satset-core/src/test/java/com/satset/transaction/service/PriceReconcileServiceTest.java
```

- [ ] **Step 2: Ganti PriceCompareRow** (tambah `seller`, `denomId`):

```java
package com.satset.transaction.model;

import java.math.BigDecimal;

public record PriceCompareRow(
        String buyerSku, String productName, String brand, String category, String seller,
        BigDecimal dbCost, BigDecimal dfCost, BigDecimal delta,
        java.util.UUID denomId, CompareStatus status) {
}
```

- [ ] **Step 3: Buang method tak terpakai** — di `DenomDomainService` hapus `findAllActive()`; di `DenomRepository` hapus baris `List<ProductDenoms> findByActiveTrueAndDeletedFalse();`.

- [ ] **Step 4: Compile — verify hijau** `mvn -f satset-core/pom.xml test-compile`. Expected: BUILD SUCCESS (tak ada referensi ke file terhapus). Kalau ada error referensi, itu bug — perbaiki.

- [ ] **Step 5: Commit**

```bash
git add -A satset-core/src/main/java/com/satset/transaction satset-core/src/test/java/com/satset/transaction \
       satset-core/src/main/resources/templates/pages/admin/pricelist.html \
       satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java \
       satset-core/src/main/java/com/satset/catalog/repository/DenomRepository.java
git commit -m "refactor(pricelist): drop standalone page, ready PriceCompareRow for per-product sync

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: CatalogCodeUtil.toCode + findOrCreate Category/Product

**Files:** Create `catalog/service/CatalogCodeUtil.java`; Modify `CategoryDomainService.java`, `ProductDomainService.java`; Test `CatalogCodeUtilTest`, `CategoryDomainServiceTest`, `ProductDomainServiceTest`.

**Interfaces:** `static String CatalogCodeUtil.toCode(String)`; `Category CategoryDomainService.findOrCreateByName(String dfName)`; `Products ProductDomainService.findOrCreateByBrand(String brand, UUID categoryId)`.

- [ ] **Step 1: Failing test CatalogCodeUtil**

```java
package com.satset.catalog.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class CatalogCodeUtilTest {
    @Test void normalizes() {
        assertThat(CatalogCodeUtil.toCode("E-Money")).isEqualTo("EMONEY");
        assertThat(CatalogCodeUtil.toCode("Mobile Legends")).isEqualTo("MOBILELEGENDS");
        assertThat(CatalogCodeUtil.toCode(" Pulsa ")).isEqualTo("PULSA");
        assertThat(CatalogCodeUtil.toCode("by.U")).isEqualTo("BYU");
    }
}
```

- [ ] **Step 2: Run — FAIL** (`-Dtest=CatalogCodeUtilTest`).
- [ ] **Step 3: Create**

```java
package com.satset.catalog.service;
/** Normalisasi nama display supplier → code katalog: UPPERCASE, buang non-alfanumerik. */
public final class CatalogCodeUtil {
    private CatalogCodeUtil() {}
    public static String toCode(String name) {
        return name == null ? "" : name.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
```

- [ ] **Step 4: Run — PASS**.
- [ ] **Step 5: Failing test findOrCreateByName** — `CategoryDomainServiceTest` (`@ExtendWith(MockitoExtension.class)`, `@Mock CategoryRepository categoryRepository; @InjectMocks CategoryDomainService service;`):

```java
@Test void findOrCreateByName_existing_returnsIt() {
    Category e = new Category(); e.setCode("EMONEY");
    when(categoryRepository.findByCode("EMONEY")).thenReturn(java.util.Optional.of(e));
    assertThat(service.findOrCreateByName("E-Money")).isSameAs(e);
    verify(categoryRepository, never()).save(any());
}
@Test void findOrCreateByName_absent_createsPrepaidActive() {
    when(categoryRepository.findByCode("EMONEY")).thenReturn(java.util.Optional.empty());
    when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));
    Category r = service.findOrCreateByName("E-Money");
    assertThat(r.getCode()).isEqualTo("EMONEY");
    assertThat(r.getName()).isEqualTo("E-Money");
    assertThat(r.getCategoryType()).isEqualTo(CategoryType.PREPAID);
    assertThat(r.isActive()).isTrue();
}
```
Import: `static org.mockito.Mockito.*`, `static org.mockito.ArgumentMatchers.any`, `com.satset.catalog.model.Category`, `com.satset.catalog.model.CategoryType`, `org.junit.jupiter.api.extension.ExtendWith`, `org.mockito.*`.

- [ ] **Step 6: Run — FAIL** (`-Dtest=CategoryDomainServiceTest`).
- [ ] **Step 7: Add findOrCreateByName** ke `CategoryDomainService`:

```java
@Transactional
@Caching(evict = {
    @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
    @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager")
})
public Category findOrCreateByName(String dfName) {
    String code = CatalogCodeUtil.toCode(dfName);
    return categoryRepository.findByCode(code).orElseGet(() -> {
        Category c = new Category();
        c.setCode(code); c.setName(dfName);
        c.setCategoryType(CategoryType.PREPAID);
        c.setActive(true); c.setDeleted(false);
        return categoryRepository.save(c);
    });
}
```

- [ ] **Step 8: Run — PASS**.
- [ ] **Step 9: Failing test findOrCreateByBrand** — `ProductDomainServiceTest` (`@Mock ProductRepository productRepository; @Mock CategoryRepository categoryRepository; @Mock DenomRepository denomRepository; @InjectMocks ProductDomainService service;`):

```java
@Test void findOrCreateByBrand_absent_createsUnderCategory() {
    java.util.UUID catId = java.util.UUID.randomUUID();
    when(productRepository.findByCode("DANA")).thenReturn(java.util.Optional.empty());
    when(productRepository.save(any(Products.class))).thenAnswer(i -> i.getArgument(0));
    Products p = service.findOrCreateByBrand("DANA", catId);
    assertThat(p.getCode()).isEqualTo("DANA");
    assertThat(p.getName()).isEqualTo("DANA");
    assertThat(p.getCategoryId()).isEqualTo(catId);
    assertThat(p.isActive()).isTrue();
}
@Test void findOrCreateByBrand_existing_returnsIt() {
    Products e = new Products(); e.setCode("DANA");
    when(productRepository.findByCode("DANA")).thenReturn(java.util.Optional.of(e));
    assertThat(service.findOrCreateByBrand("DANA", java.util.UUID.randomUUID())).isSameAs(e);
    verify(productRepository, never()).save(any());
}
```

- [ ] **Step 10: Run — FAIL**.
- [ ] **Step 11: Add findOrCreateByBrand** ke `ProductDomainService`:

```java
@Transactional
@CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager")
public Products findOrCreateByBrand(String brand, UUID categoryId) {
    String code = CatalogCodeUtil.toCode(brand);
    return productRepository.findByCode(code).orElseGet(() -> {
        Products p = new Products();
        p.setCode(code); p.setName(brand); p.setCategoryId(categoryId);
        p.setActive(true); p.setDeleted(false);
        return productRepository.save(p);
    });
}
```

- [ ] **Step 12: Run — PASS** (`-Dtest='CatalogCodeUtilTest,CategoryDomainServiceTest,ProductDomainServiceTest'`).
- [ ] **Step 13: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/service/CatalogCodeUtil.java \
        satset-core/src/main/java/com/satset/catalog/service/CategoryDomainService.java \
        satset-core/src/main/java/com/satset/catalog/service/ProductDomainService.java \
        satset-core/src/test/java/com/satset/catalog/service/CatalogCodeUtilTest.java \
        satset-core/src/test/java/com/satset/catalog/service/CategoryDomainServiceTest.java \
        satset-core/src/test/java/com/satset/catalog/service/ProductDomainServiceTest.java
git commit -m "feat(catalog): toCode + findOrCreate Category/Product

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: DenomDomainService — supplier create/update/deactivate + finder

**Files:** Modify `catalog/service/DenomDomainService.java`; Test `catalog/service/DenomDomainServiceTest.java`.

**Interfaces:** `List<ProductDenoms> findActiveByProductId(UUID)`; `ProductDenoms createFromSupplier(UUID productId, String sku, String name, BigDecimal cost)`; `void updateCostById(UUID, BigDecimal)`; `void deactivateById(UUID)`.

> `DenomRepository.findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID)` sudah ada — dipakai `findActiveByProductId`.

- [ ] **Step 1: Failing tests** — `DenomDomainServiceTest` (`@Mock DenomRepository denomRepository; @Mock DenomMetaRepository metaRepository; @Mock ProductRepository productRepository; @InjectMocks DenomDomainService service;`):

```java
@Test void createFromSupplier_keepsCodeLowercase() {
    java.util.UUID pid = java.util.UUID.randomUUID();
    when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(i -> i.getArgument(0));
    ProductDenoms d = service.createFromSupplier(pid, "dana20", "DANA 20.000", new java.math.BigDecimal("20500"));
    assertThat(d.getCode()).isEqualTo("dana20");
    assertThat(d.getProductId()).isEqualTo(pid);
    assertThat(d.getBasePrice()).isEqualByComparingTo("20500");
    assertThat(d.getPrice()).isNull();
    assertThat(d.getDenomType()).isEqualTo(DenomType.FIXED_DENOM);
    assertThat(d.isActive()).isTrue();
}
@Test void updateCostById_setsBasePrice() {
    java.util.UUID id = java.util.UUID.randomUUID();
    ProductDenoms e = new ProductDenoms(); e.setId(id); e.setBasePrice(new java.math.BigDecimal("5000"));
    when(denomRepository.findById(id)).thenReturn(java.util.Optional.of(e));
    when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(i -> i.getArgument(0));
    service.updateCostById(id, new java.math.BigDecimal("5450"));
    assertThat(e.getBasePrice()).isEqualByComparingTo("5450");
}
@Test void deactivateById_setsInactive() {
    java.util.UUID id = java.util.UUID.randomUUID();
    ProductDenoms e = new ProductDenoms(); e.setId(id); e.setActive(true);
    when(denomRepository.findById(id)).thenReturn(java.util.Optional.of(e));
    when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(i -> i.getArgument(0));
    service.deactivateById(id);
    assertThat(e.isActive()).isFalse();
}
```
Import: `com.satset.catalog.model.DenomType`, `com.satset.catalog.model.ProductDenoms`, `static org.mockito.Mockito.*`, `static org.mockito.ArgumentMatchers.any`.

- [ ] **Step 2: Run — FAIL** (`-Dtest=DenomDomainServiceTest`).
- [ ] **Step 3: Add methods** (import `com.satset.catalog.model.DenomType`, `java.math.BigDecimal`):

```java
public List<ProductDenoms> findActiveByProductId(UUID productId) {
    return denomRepository.findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(productId);
}

@Transactional
public ProductDenoms createFromSupplier(UUID productId, String sku, String name, BigDecimal cost) {
    ProductDenoms d = new ProductDenoms();
    d.setProductId(productId);
    d.setCode(sku);                 // apa adanya — JANGAN uppercase
    d.setName(name);
    d.setDenomType(DenomType.FIXED_DENOM);
    d.setBasePrice(cost);
    d.setActive(true);
    d.setDeleted(false);
    return denomRepository.save(d);
}

@Transactional
public void updateCostById(UUID denomId, BigDecimal cost) {
    ProductDenoms d = denomRepository.findById(denomId)
            .orElseThrow(() -> new ResourceNotFoundException("Denom", denomId));
    d.setBasePrice(cost);
    denomRepository.save(d);
}

@Transactional
public void deactivateById(UUID denomId) {
    ProductDenoms d = denomRepository.findById(denomId)
            .orElseThrow(() -> new ResourceNotFoundException("Denom", denomId));
    d.setActive(false);
    denomRepository.save(d);
}
```

- [ ] **Step 4: Run — PASS**.
- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java \
        satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java
git commit -m "feat(catalog): denom supplier create/updateCost/deactivate + findActiveByProductId

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: CatalogSyncService — syncCategories/Products/Denoms + reconcileForProduct

**Files:** Create `transaction/service/SyncResult.java`, `transaction/service/CatalogSyncService.java`; Test `transaction/service/CatalogSyncServiceTest.java`.

**Interfaces:**
- Consumes: `DigiflazzClient.fetchPriceList()`; `CategoryDomainService.{findById, findAllForAdmin, findOrCreateByName}`; `ProductDomainService.{findById, findByCategoryForAdmin, findOrCreateByBrand}`; `DenomDomainService.{findActiveByProductId, createFromSupplier, updateCostById, deactivateById}`; `CatalogCodeUtil.toCode`.
- Produces: `record SyncResult(int created, int costUpdated, int deactivated, int skipped, int failed)`; `SyncResult syncCategories()`; `SyncResult syncProducts(UUID categoryId)`; `SyncResult syncDenoms(UUID productId)`; `List<PriceCompareRow> reconcileForProduct(UUID productId)`.

- [ ] **Step 1: Failing tests**

```java
package com.satset.transaction.service;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.service.CategoryDomainService;
import com.satset.catalog.service.DenomDomainService;
import com.satset.catalog.service.ProductDomainService;
import com.satset.transaction.client.DigiflazzClient;
import com.satset.transaction.model.CompareStatus;
import com.satset.transaction.model.PriceCompareRow;
import com.satset.transaction.model.PriceListItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogSyncServiceTest {

    @Mock DigiflazzClient digiflazz;
    @Mock CategoryDomainService categoryService;
    @Mock ProductDomainService productService;
    @Mock DenomDomainService denomService;

    private CatalogSyncService service() {
        return new CatalogSyncService(digiflazz, categoryService, productService, denomService);
    }

    private static PriceListItem df(String sku, String name, String cat, String brand, long price) {
        return new PriceListItem(name, cat, brand, "Umum", sku, price, true, true, false, "0", "Ki***", "");
    }
    private static ProductDenoms denom(UUID id, String code, BigDecimal base) {
        ProductDenoms d = new ProductDenoms(); d.setId(id); d.setCode(code); d.setName("n"+code);
        d.setBasePrice(base); d.setActive(true); return d;
    }

    @Test void syncCategories_createsMissingOnly() {
        when(digiflazz.fetchPriceList()).thenReturn(List.of(
                df("a","A","Pulsa","XL",1), df("b","B","E-Money","DANA",2), df("c","C","Pulsa","AXIS",3)));
        Category existing = new Category(); existing.setCode("PULSA");
        when(categoryService.findAllForAdmin()).thenReturn(List.of(existing));
        SyncResult r = service().syncCategories();
        verify(categoryService).findOrCreateByName("E-Money");
        verify(categoryService, never()).findOrCreateByName("Pulsa");
        assertThat(r.created()).isEqualTo(1);
        assertThat(r.skipped()).isEqualTo(1);
    }

    @Test void syncProducts_createsBrandsInOpenCategoryOnly() {
        UUID catId = UUID.randomUUID();
        Category cat = new Category(); cat.setId(catId); cat.setCode("PULSA");
        when(categoryService.findById(catId)).thenReturn(Optional.of(cat));
        when(digiflazz.fetchPriceList()).thenReturn(List.of(
                df("a","A","Pulsa","XL",1), df("b","B","E-Money","DANA",2), df("c","C","Pulsa","XL",3)));
        when(productService.findByCategoryForAdmin(catId)).thenReturn(List.of());
        SyncResult r = service().syncProducts(catId);
        verify(productService).findOrCreateByBrand("XL", catId);
        verify(productService, never()).findOrCreateByBrand(eq("DANA"), any());
        assertThat(r.created()).isEqualTo(1); // XL sekali (distinct)
    }

    @Test void reconcileForProduct_filtersByBrandCode_computesStatus() {
        UUID pid = UUID.randomUUID();
        Products p = new Products(); p.setId(pid); p.setCode("XL");
        when(productService.findById(pid)).thenReturn(Optional.of(p));
        when(digiflazz.fetchPriceList()).thenReturn(List.of(
                df("x100","XL 100","Pulsa","XL",98000),   // matched->NAIK
                df("x5","XL 5","Pulsa","XL",5500),         // BARU
                df("dana20","D","E-Money","DANA",20000))); // beda brand, di-skip
        UUID d1 = UUID.randomUUID();
        when(denomService.findActiveByProductId(pid)).thenReturn(List.of(
                denom(d1, "X100", new BigDecimal("97000")),        // matched
                denom(UUID.randomUUID(), "XOLD", new BigDecimal("1000")))); // HILANG
        List<PriceCompareRow> rows = service().reconcileForProduct(pid);
        assertThat(rows).extracting(PriceCompareRow::status)
                .containsExactlyInAnyOrder(CompareStatus.NAIK, CompareStatus.BARU, CompareStatus.HILANG);
        assertThat(rows).noneMatch(r -> "dana20".equals(r.buyerSku()));
    }

    @Test void syncDenoms_appliesMirror() {
        UUID pid = UUID.randomUUID();
        Products p = new Products(); p.setId(pid); p.setCode("XL");
        when(productService.findById(pid)).thenReturn(Optional.of(p));
        when(digiflazz.fetchPriceList()).thenReturn(List.of(
                df("x100","XL 100","Pulsa","XL",98000),
                df("x5","XL 5","Pulsa","XL",5500)));
        UUID d1 = UUID.randomUUID(), dOld = UUID.randomUUID();
        when(denomService.findActiveByProductId(pid)).thenReturn(List.of(
                denom(d1, "X100", new BigDecimal("97000")),
                denom(dOld, "XOLD", new BigDecimal("1000"))));
        SyncResult r = service().syncDenoms(pid);
        verify(denomService).updateCostById(d1, new BigDecimal("98000"));
        verify(denomService).createFromSupplier(pid, "x5", "XL 5", new BigDecimal("5500"));
        verify(denomService).deactivateById(dOld);
        assertThat(r.costUpdated()).isEqualTo(1);
        assertThat(r.created()).isEqualTo(1);
        assertThat(r.deactivated()).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run — FAIL** (`-Dtest=CatalogSyncServiceTest`), kelas belum ada.

- [ ] **Step 3: Create SyncResult**

```java
package com.satset.transaction.service;
/** Ringkasan hasil sync katalog dgn Digiflazz. */
public record SyncResult(int created, int costUpdated, int deactivated, int skipped, int failed) {}
```

- [ ] **Step 4: Create CatalogSyncService**

```java
package com.satset.transaction.service;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.service.CatalogCodeUtil;
import com.satset.catalog.service.CategoryDomainService;
import com.satset.catalog.service.DenomDomainService;
import com.satset.catalog.service.ProductDomainService;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.client.DigiflazzClient;
import com.satset.transaction.model.CompareStatus;
import com.satset.transaction.model.PriceCompareRow;
import com.satset.transaction.model.PriceListItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Sync katalog per-level dgn Digiflazz. Recompute dari DF fresh tiap aksi. */
@Slf4j
@Service
public class CatalogSyncService {

    private final DigiflazzClient digiflazz;
    private final CategoryDomainService categoryService;
    private final ProductDomainService productService;
    private final DenomDomainService denomService;

    public CatalogSyncService(DigiflazzClient digiflazz, CategoryDomainService categoryService,
                              ProductDomainService productService, DenomDomainService denomService) {
        this.digiflazz = digiflazz;
        this.categoryService = categoryService;
        this.productService = productService;
        this.denomService = denomService;
    }

    /** Buat Category dari kategori DF yang belum ada. */
    public SyncResult syncCategories() {
        Set<String> existing = categoryService.findAllForAdmin().stream()
                .map(c -> c.getCode()).collect(Collectors.toSet());
        Set<String> seen = new HashSet<>();
        int created = 0, skipped = 0, failed = 0;
        for (PriceListItem it : digiflazz.fetchPriceList()) {
            String code = CatalogCodeUtil.toCode(it.category());
            if (!seen.add(code)) continue;
            try {
                if (existing.contains(code)) { skipped++; }
                else { categoryService.findOrCreateByName(it.category()); created++; }
            } catch (Exception e) {
                log.error("syncCategories gagal utk {}", it.category(), e);
                failed++;
            }
        }
        return new SyncResult(created, 0, 0, skipped, failed);
    }

    /** Buat Product (brand) DF yang belum ada, dalam satu kategori. */
    public SyncResult syncProducts(UUID categoryId) {
        Category cat = categoryService.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        Set<String> existing = productService.findByCategoryForAdmin(categoryId).stream()
                .map(p -> p.getCode()).collect(Collectors.toSet());
        Set<String> seen = new HashSet<>();
        int created = 0, skipped = 0, failed = 0;
        for (PriceListItem it : digiflazz.fetchPriceList()) {
            if (!CatalogCodeUtil.toCode(it.category()).equals(cat.getCode())) continue;
            String code = CatalogCodeUtil.toCode(it.brand());
            if (!seen.add(code)) continue;
            try {
                if (existing.contains(code)) { skipped++; }
                else { productService.findOrCreateByBrand(it.brand(), categoryId); created++; }
            } catch (Exception e) {
                log.error("syncProducts gagal utk brand {}", it.brand(), e);
                failed++;
            }
        }
        return new SyncResult(created, 0, 0, skipped, failed);
    }

    /** Full mirror denom untuk satu produk (brand): create/updateCost/deactivate. */
    public SyncResult syncDenoms(UUID productId) {
        List<PriceCompareRow> rows = reconcileForProduct(productId);
        int created = 0, costUpdated = 0, deactivated = 0, skipped = 0, failed = 0;
        for (PriceCompareRow r : rows) {
            try {
                switch (r.status()) {
                    case BARU -> { denomService.createFromSupplier(productId, r.buyerSku(), r.productName(), r.dfCost()); created++; }
                    case NAIK, TURUN -> { denomService.updateCostById(r.denomId(), r.dfCost()); costUpdated++; }
                    case HILANG -> { denomService.deactivateById(r.denomId()); deactivated++; }
                    case SAMA -> skipped++;
                }
            } catch (Exception e) {
                log.error("syncDenoms gagal utk SKU {}", r.buyerSku(), e);
                failed++;
            }
        }
        return new SyncResult(created, costUpdated, deactivated, skipped, failed);
    }

    /** Banding harga beli denom produk vs DF (buat kolom delta di halaman denom). */
    public List<PriceCompareRow> reconcileForProduct(UUID productId) {
        Products product = productService.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        String productCode = product.getCode();

        // SKU DF utk brand produk ini (dedup by sku, harga terendah)
        Map<String, PriceListItem> uniq = new LinkedHashMap<>();
        for (PriceListItem it : digiflazz.fetchPriceList()) {
            if (!CatalogCodeUtil.toCode(it.brand()).equals(productCode)) continue;
            uniq.merge(it.buyerSkuCode().toUpperCase(), it, (a, b) -> a.price() <= b.price() ? a : b);
        }

        Map<String, ProductDenoms> byCode = denomService.findActiveByProductId(productId).stream()
                .collect(Collectors.toMap(d -> d.getCode().toUpperCase(), Function.identity(), (a, b) -> a));

        List<PriceCompareRow> rows = new ArrayList<>();
        Set<String> matched = new HashSet<>();
        for (PriceListItem it : uniq.values()) {
            BigDecimal dfCost = BigDecimal.valueOf(it.price());
            ProductDenoms denom = byCode.get(it.buyerSkuCode().toUpperCase());
            if (denom == null) {
                rows.add(new PriceCompareRow(it.buyerSkuCode(), it.productName(), it.brand(), it.category(),
                        it.sellerName(), null, dfCost, null, null, CompareStatus.BARU));
                continue;
            }
            matched.add(denom.getCode().toUpperCase());
            BigDecimal dbCost = denom.getBasePrice();
            CompareStatus status;
            BigDecimal delta = null;
            if (dbCost == null) { status = CompareStatus.NAIK; }
            else {
                int cmp = dbCost.compareTo(dfCost);
                status = cmp == 0 ? CompareStatus.SAMA : cmp < 0 ? CompareStatus.NAIK : CompareStatus.TURUN;
                delta = dfCost.subtract(dbCost);
            }
            rows.add(new PriceCompareRow(it.buyerSkuCode(), it.productName(), it.brand(), it.category(),
                    it.sellerName(), dbCost, dfCost, delta, denom.getId(), status));
        }
        byCode.forEach((code, denom) -> {
            if (!matched.contains(code)) {
                rows.add(new PriceCompareRow(denom.getCode(), denom.getName(), product.getName(), null,
                        null, denom.getBasePrice(), null, null, denom.getId(), CompareStatus.HILANG));
            }
        });
        return rows;
    }
}
```

- [ ] **Step 5: Run — PASS** (`-Dtest=CatalogSyncServiceTest`).
- [ ] **Step 6: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/service/SyncResult.java \
        satset-core/src/main/java/com/satset/transaction/service/CatalogSyncService.java \
        satset-core/src/test/java/com/satset/transaction/service/CatalogSyncServiceTest.java
git commit -m "feat(pricelist): CatalogSyncService per-level (categories/products/denoms) + reconcileForProduct

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: CatalogSyncController — REST endpoints

**Files:** Create `transaction/web/CatalogSyncController.java`; Test `transaction/web/CatalogSyncControllerTest.java`.

**Interfaces:** Consumes `CatalogSyncService`. Produces:
- `POST /api/admin/catalog/sync/categories` → `SyncResult`
- `POST /api/admin/catalog/categories/{categoryId}/sync/products` → `SyncResult`
- `POST /api/admin/catalog/products/{productId}/sync/denoms` → `SyncResult`
- `GET  /api/admin/catalog/products/{productId}/pricelist-compare` → `List<PriceCompareRow>`

- [ ] **Step 1: Failing test**

```java
package com.satset.transaction.web;

import com.satset.transaction.service.CatalogSyncService;
import com.satset.transaction.service.SyncResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CatalogSyncControllerTest {

    @Mock CatalogSyncService sync;
    private MockMvc mockMvc() { return MockMvcBuilders.standaloneSetup(new CatalogSyncController(sync)).build(); }

    @Test void syncCategories_json() throws Exception {
        when(sync.syncCategories()).thenReturn(new SyncResult(2, 0, 0, 3, 0));
        mockMvc().perform(post("/api/admin/catalog/sync/categories"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(2));
    }
    @Test void syncProducts_passesCategoryId() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.syncProducts(id)).thenReturn(new SyncResult(1, 0, 0, 0, 0));
        mockMvc().perform(post("/api/admin/catalog/categories/" + id + "/sync/products"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.created").value(1));
    }
    @Test void syncDenoms_passesProductId() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.syncDenoms(id)).thenReturn(new SyncResult(1, 2, 1, 0, 0));
        mockMvc().perform(post("/api/admin/catalog/products/" + id + "/sync/denoms"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.costUpdated").value(2));
    }
    @Test void compare_returnsRows() throws Exception {
        UUID id = UUID.randomUUID();
        when(sync.reconcileForProduct(id)).thenReturn(List.of());
        mockMvc().perform(get("/api/admin/catalog/products/" + id + "/pricelist-compare"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run — FAIL** (`-Dtest=CatalogSyncControllerTest`).
- [ ] **Step 3: Create controller** — verifikasi konstanta role di `OmniConstants` (`PERM_MANAGE_CATEGORIES`, `PERM_MANAGE_PRODUCTS`, `PERM_MANAGE_DENOMS`; templates pakai `REALM_manage_categories/products/denoms`). Kalau nama konstanta beda, sesuaikan.

```java
package com.satset.transaction.web;

import com.satset.shared.constant.SatsetConstants;
import com.satset.transaction.model.PriceCompareRow;
import com.satset.transaction.service.CatalogSyncService;
import com.satset.transaction.service.SyncResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Endpoint sync katalog per-level dgn Digiflazz. */
@RestController
@RequestMapping("/api/admin/catalog")
public class CatalogSyncController {

    private final CatalogSyncService sync;

    public CatalogSyncController(CatalogSyncService sync) { this.sync = sync; }

    @PostMapping("/sync/categories")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public SyncResult syncCategories() { return sync.syncCategories(); }

    @PostMapping("/categories/{categoryId}/sync/products")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public SyncResult syncProducts(@PathVariable UUID categoryId) { return sync.syncProducts(categoryId); }

    @PostMapping("/products/{productId}/sync/denoms")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public SyncResult syncDenoms(@PathVariable UUID productId) { return sync.syncDenoms(productId); }

    @GetMapping("/products/{productId}/pricelist-compare")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public List<PriceCompareRow> compare(@PathVariable UUID productId) { return sync.reconcileForProduct(productId); }
}
```

- [ ] **Step 4: Run — PASS**.
- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/transaction/web/CatalogSyncController.java \
        satset-core/src/test/java/com/satset/transaction/web/CatalogSyncControllerTest.java
git commit -m "feat(pricelist): CatalogSyncController per-level endpoints + compare

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: UI — tombol sync di halaman katalog + delta di denom

**Files:** Modify `categories.html`, `products.html`, `denoms.html`. (Tak ada unit test; verifikasi manual Task 8.) Ikuti pola existing: `Alpine.store('confirm').open({...})→bool`, `Alpine.store('toast').success/error`, `fetch` (CSRF auto), reload halaman setelah sukses.

- [ ] **Step 1: categories.html** — di header (dekat tombol "Tambah Kategori", baris ~27) tambah tombol sebelum/sesudahnya:

```html
<button sec:authorize="hasRole('REALM_manage_categories')" class="btn btn-outline tap"
        @click="syncCategories()" :disabled="syncing">
    <span x-show="syncing" class="loading loading-spinner loading-xs"></span>
    Sync Kategori DF
</button>
```
Di `categoryManager()` (return object) tambah properti + method:

```javascript
syncing: false,
async syncCategories() {
    const ok = await Alpine.store('confirm').open({
        title: 'Sync Kategori Digiflazz',
        message: 'Buat kategori dari Digiflazz yang belum ada di katalog?',
        confirmText: 'Sync', confirmClass: 'btn-primary'
    });
    if (!ok) return;
    this.syncing = true;
    try {
        const res = await fetch('/api/admin/catalog/sync/categories', { method: 'POST' });
        if (!res.ok) throw new Error('gagal');
        const r = await res.json();
        Alpine.store('toast').success(`Sync kategori: ${r.created} baru, ${r.skipped} sudah ada`);
        await this.loadCategories();
    } catch (e) {
        Alpine.store('toast').error('Gagal sync kategori');
    } finally { this.syncing = false; }
}
```

- [ ] **Step 2: products.html** — tombol di header (dekat "Tambah Produk", baris ~37). Aktif hanya bila satu kategori dipilih (`filterCategoryId`):

```html
<button sec:authorize="hasRole('REALM_manage_products')" class="btn btn-outline tap"
        @click="syncProducts()" :disabled="syncing || !filterCategoryId"
        :title="filterCategoryId ? '' : 'Pilih kategori dulu'">
    <span x-show="syncing" class="loading loading-spinner loading-xs"></span>
    Sync Produk DF
</button>
```
Di `productManager()` tambah:

```javascript
syncing: false,
async syncProducts() {
    if (!this.filterCategoryId) { Alpine.store('toast').error('Pilih kategori dulu'); return; }
    const ok = await Alpine.store('confirm').open({
        title: 'Sync Produk Digiflazz',
        message: 'Buat brand/produk Digiflazz yang belum ada di kategori ini?',
        confirmText: 'Sync', confirmClass: 'btn-primary'
    });
    if (!ok) return;
    this.syncing = true;
    try {
        const res = await fetch(`/api/admin/catalog/categories/${this.filterCategoryId}/sync/products`, { method: 'POST' });
        if (!res.ok) throw new Error('gagal');
        const r = await res.json();
        Alpine.store('toast').success(`Sync produk: ${r.created} baru, ${r.skipped} sudah ada`);
        await this.loadProducts();
    } catch (e) {
        Alpine.store('toast').error('Gagal sync produk');
    } finally { this.syncing = false; }
}
```

- [ ] **Step 3: denoms.html — tombol Sync Denom** di header (dekat "Tambah Denom", baris ~35):

```html
<button sec:authorize="hasRole('REALM_manage_denoms')" class="btn btn-outline tap"
        @click="syncDenoms()" :disabled="syncing">
    <span x-show="syncing" class="loading loading-spinner loading-xs"></span>
    Sync Denom DF
</button>
```

- [ ] **Step 4: denoms.html — muat delta DF + kolom** — di `denomManager()` tambah state `compare: {}` (map SKU-upper→row), `syncing:false`, `loadCompare()`, `syncDenoms()`; panggil `loadCompare()` di init. Tambah di object:

```javascript
syncing: false,
compareBySku: {},

async loadCompare() {
    try {
        const res = await fetch(`/api/admin/catalog/products/${this.productId}/pricelist-compare`);
        if (!res.ok) return;
        const rows = await res.json();
        const map = {};
        rows.forEach(r => { if (r.buyerSku) map[r.buyerSku.toUpperCase()] = r; });
        this.compareBySku = map;
    } catch (e) { /* diam: delta opsional */ }
},
dfInfo(code) { return this.compareBySku[(code || '').toUpperCase()] || null; },

async syncDenoms() {
    const ok = await Alpine.store('confirm').open({
        title: 'Sync Denom Digiflazz',
        message: 'Mirror denom produk ini ke Digiflazz: buat SKU baru, update harga beli, nonaktifkan yang hilang dari DF. Lanjut?',
        confirmText: 'Sync', confirmClass: 'btn-primary'
    });
    if (!ok) return;
    this.syncing = true;
    try {
        const res = await fetch(`/api/admin/catalog/products/${this.productId}/sync/denoms`, { method: 'POST' });
        if (!res.ok) throw new Error('gagal');
        const r = await res.json();
        Alpine.store('toast').success(`Sync denom: ${r.created} baru, ${r.costUpdated} update, ${r.deactivated} nonaktif, ${r.failed} gagal`);
        await this.loadDenoms();
        await this.loadCompare();
    } catch (e) {
        Alpine.store('toast').error('Gagal sync denom');
    } finally { this.syncing = false; }
}
```
Cari blok `x-init` / awal pada root `denomManager()` (atau tambahkan `x-init="loadCompare()"` di elemen root `layout:fragment="content"`). Di tabel denom, tambah 1 kolom "Harga DF" yang nampilin `dfInfo(d.code)`:

```html
<td class="text-right">
    <template x-if="dfInfo(d.code)">
        <span :class="dfInfo(d.code).status === 'NAIK' ? 'text-error' : dfInfo(d.code).status === 'TURUN' ? 'text-success' : ''"
              x-text="dfInfo(d.code).dfCost != null ? new Intl.NumberFormat('id-ID').format(dfInfo(d.code).dfCost) : '-'"></span>
    </template>
    <template x-if="!dfInfo(d.code)"><span class="text-base-content/40">-</span></template>
</td>
```
Tambah `<th class="text-right">Harga DF</th>` di header tabel denom yang sesuai.

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/categories.html \
        satset-core/src/main/resources/templates/pages/admin/catalog/products.html \
        satset-core/src/main/resources/templates/pages/admin/catalog/denoms.html
git commit -m "feat(pricelist): sync buttons per-level di halaman katalog + delta DF di denom

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: Regression + verifikasi end-to-end

- [ ] **Step 1: Full affected test run**

```bash
mvn -f satset-core/pom.xml -Dtest='DigiflazzClientTest,CatalogCodeUtilTest,CategoryDomainServiceTest,ProductDomainServiceTest,DenomDomainServiceTest,CatalogSyncServiceTest,CatalogSyncControllerTest,ModularityTest' test
```
Expected: `BUILD SUCCESS`. `ModularityTest` lolos (boundary transaction→catalog.service). Kalau `ModularityTest` gagal karena `CatalogSyncService`/`CatalogSyncController` (transaction) impor catalog `service` — itu boleh (service = API antar-slice); yang dilarang impor `catalog.repository`. Pastikan tak ada impor repository di transaction.

- [ ] **Step 2: Boot + manual E2E** — `.env` root ada `DIGIFLAZZ_USERNAME/DIGIFLAZZ_DEV_KEY/PROXY_PASS`. `mvn -f satset-core/pom.xml spring-boot:run`. Login admin.
  - `/admin/catalog/categories` → **Sync Kategori DF** → toast `X baru`; cek: `docker exec postgres-satset psql -U admin -d satset_go -c "SELECT code,name FROM categories ORDER BY created_at DESC LIMIT 5;"`.
  - `/admin/catalog/products?categoryId=<PULSA>` → **Sync Produk DF** → brand (XL/TELKOMSEL/AXIS...) kebentuk di kategori itu.
  - Buka denom salah satu produk hasil sync → kolom **Harga DF** kebaca; klik **Sync Denom DF** → toast ringkasan; cek denom: `code` lowercase, `base_price` = harga DF, `price` NULL, `active=t`; denom lama tak-di-DF → `active=f`.
  - Matikan proxy (`PROXY_PASS` kosong) → klik sync → toast gagal (bukan 500).

- [ ] **Step 3: Update graph + task tracking**

```bash
graphify update .
```
Tandai `[x]` di `Tasks.md` + Google Tasks list `Z184dEJwWFlUSG1GTkdIYQ`.

- [ ] **Step 4: Commit sisa (Tasks.md/graph)**

```bash
git add Tasks.md graphify-out
git commit -m "chore(pricelist): update tasks + graph after catalog sync feature

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Self-Review Notes

- **Spec coverage (revisi per-level):** kategori create-missing → Task 5 `syncCategories`; produk create-missing per kategori → `syncProducts`; denom full mirror per produk → `syncDenoms`; delta di denom page → `reconcileForProduct` + GET compare (Task 6) + UI (Task 7); code lowercase → Task 4; halaman /admin/pricelist dibuang → Task 2.
- **Boundary:** `CatalogSyncService`/`Controller` (transaction) impor catalog `service` saja, bukan `repository` → ModularityTest.
- **Type consistency:** `PriceCompareRow(buyerSku, productName, brand, category, seller, dbCost, dfCost, delta, denomId, status)` dipakai konsisten Task 2/5/6. `SyncResult(created, costUpdated, deactivated, skipped, failed)` konsisten Task 5/6/7.
- **Caveat (didokumentasikan):** (a) kategori/produk legacy code ≠ `toCode(nama DF)` → bisa duplikat, admin merge. (b) `syncDenoms` mirror penuh → denom manual di bawah produk yang tak ada di DF akan dinonaktifkan. (c) `syncDenoms` cuma nemu SKU bila `product.code == toCode(brand)` — produk hasil `syncProducts` otomatis konsisten; produk legacy dgn code beda tak akan match.
```
