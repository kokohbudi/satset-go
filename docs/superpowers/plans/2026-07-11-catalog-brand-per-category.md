# Catalog "Product = Brand × Category" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let one brand (e.g. TELKOMSEL) exist independently under multiple categories by moving `products` uniqueness from global `code` to `(category_id, code)` and making every product lookup category-scoped.

**Architecture:** Denormalized "Product = Brand × Category" (Approach A). `products` already carries `category_id`; we swap the unique constraint, add category-scoped repository queries, migrate every global `findByCode` caller (sync, storefront, CRUD), and nest the storefront product/denom routes under their category. Additive first (new repo methods), remove the old global methods last so every task compiles.

**Tech Stack:** Spring Boot 4, Hibernate 7 (JPA), PostgreSQL, JUnit 5 + AssertJ + Mockito, Thymeleaf + Alpine.js.

## Global Constraints

- Java 25, Spring Boot 4.0.1, Maven. Build/test via system `mvn` (no `./mvnw`).
- Module: `satset-core`. Test cmd form: `mvn -pl satset-core test -Dtest=<Class>`.
- TDD strict: Red → Green → Refactor. Failing test before implementation.
- UUID ids (`@UuidGenerator`), soft delete (`deleted`), optimistic lock (`@Version`).
- `ddl-auto=update` in dev does NOT drop an existing unique index; `validate` in prod does NOT check unique constraints → constraint migrations are manual SQL both envs.
- Product `code` is stored uppercased/trimmed via `CatalogCodeUtil.toCode(...)`.
- `categoryId` on `Products` is a **raw UUID column**, not a mapped relation → no derived-query navigation through `Category`; resolve category code → id via `CategoryRepository.findByCode` then query by `categoryId`.
- DB access: `docker exec -i postgres-satset psql -U admin -d satset_go -c "..."` (the `-i` is required).
- Errors: never expose `e.getMessage()` to client; `log.error()` only.
- Git deletion: `git rm`.

---

### Task 1: Composite unique constraint `(category_id, code)` + category-scoped repo queries

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/model/Products.java:16,28`
- Modify: `satset-core/src/main/java/com/satset/catalog/repository/ProductRepository.java`
- Test: `satset-core/src/test/java/com/satset/catalog/repository/ProductUniquenessTest.java` (create)

**Interfaces:**
- Produces: `ProductRepository.findByCategoryIdAndCode(UUID categoryId, String code) : Optional<Products>`
- Produces: `ProductRepository.existsByCategoryIdAndCodeAndIdNot(UUID categoryId, String code, UUID id) : boolean`
- The old global `findByCode(String)` and `existsByCodeAndIdNot(String, UUID)` remain for now (removed in Task 4).

- [ ] **Step 1: Apply the dev-DB migration** (constraint test runs against dev PG; do this first so the schema matches the new entity)

Find the old constraint name:
```bash
docker exec -i postgres-satset psql -U admin -d satset_go -c "\d products" | grep -i unique
```
Then (substitute the real old name for `<old>`):
```bash
docker exec -i postgres-satset psql -U admin -d satset_go -c "
ALTER TABLE products DROP CONSTRAINT <old>;
ALTER TABLE products ADD CONSTRAINT uq_products_category_code UNIQUE (category_id, code);"
```
Expected: `ALTER TABLE` twice. Verify:
```bash
docker exec -i postgres-satset psql -U admin -d satset_go -c "\d products" | grep uq_products_category_code
```

- [ ] **Step 2: Write the failing constraint test**

`ProductUniquenessTest.java`:
```java
package com.satset.catalog.repository;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.Products;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ProductUniquenessTest {

    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;

    private UUID newCategory(String code) {
        Category c = new Category();
        c.setCode(code); c.setName(code); c.setActive(true); c.setDeleted(false);
        return categoryRepository.save(c).getId();
    }

    private Products product(UUID categoryId, String code) {
        Products p = new Products();
        p.setCategoryId(categoryId); p.setCode(code); p.setName(code);
        p.setActive(true); p.setDeleted(false);
        return p;
    }

    @Test
    void sameBrandCode_inTwoCategories_bothPersist() {
        UUID pulsa = newCategory("T1PULSA");
        UUID data = newCategory("T1DATA");
        productRepository.saveAndFlush(product(pulsa, "TELKOMSEL"));
        productRepository.saveAndFlush(product(data, "TELKOMSEL"));
        assertThat(productRepository.findByCategoryIdAndCode(pulsa, "TELKOMSEL")).isPresent();
        assertThat(productRepository.findByCategoryIdAndCode(data, "TELKOMSEL")).isPresent();
    }

    @Test
    void sameBrandCode_twiceInOneCategory_violatesUnique() {
        UUID pulsa = newCategory("T1PULSA2");
        productRepository.saveAndFlush(product(pulsa, "XL"));
        assertThatThrownBy(() -> productRepository.saveAndFlush(product(pulsa, "XL")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

- [ ] **Step 3: Run the test, verify it fails to compile**

Run: `mvn -pl satset-core test -Dtest=ProductUniquenessTest`
Expected: COMPILE FAIL — `findByCategoryIdAndCode` not defined on `ProductRepository`.

- [ ] **Step 4: Add the repo methods**

In `ProductRepository.java`, add:
```java
    Optional<Products> findByCategoryIdAndCode(UUID categoryId, String code);

    boolean existsByCategoryIdAndCodeAndIdNot(UUID categoryId, String code, UUID id);
```
(Keep the existing `findByCode` and `existsByCodeAndIdNot` — removed in Task 4.)

- [ ] **Step 5: Change the entity constraint**

In `Products.java`:
- Line 16 `@Table(name = "products")` →
  ```java
  @Table(name = "products", uniqueConstraints =
          @UniqueConstraint(name = "uq_products_category_code", columnNames = {"category_id", "code"}))
  ```
- Line 28 `@Column(unique = true, nullable = false, length = 50)` →
  `@Column(nullable = false, length = 50)`

- [ ] **Step 6: Run the test, verify it passes**

Run: `mvn -pl satset-core test -Dtest=ProductUniquenessTest`
Expected: PASS (2 tests).

- [ ] **Step 7: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/model/Products.java \
  satset-core/src/main/java/com/satset/catalog/repository/ProductRepository.java \
  satset-core/src/test/java/com/satset/catalog/repository/ProductUniquenessTest.java
git commit -m "feat(catalog): products UNIQUE(category_id, code) + category-scoped repo queries

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2: `findOrCreateByBrand` + sync existence check become category-scoped (core fix)

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/service/ProductDomainService.java:112-127`
- Modify: `satset-core/src/main/java/com/satset/supplier/service/CatalogSyncService.java:102`
- Test: `satset-core/src/test/java/com/satset/catalog/service/ProductDomainServiceTest.java`
- Test: `satset-core/src/test/java/com/satset/supplier/service/CatalogSyncServiceTest.java`

**Interfaces:**
- Produces: `ProductDomainService.findByCategoryAndCode(String categoryCode, String code) : Optional<Products>` (resolves category code → id via `CategoryRepository.findByCode`, then `productRepository.findByCategoryIdAndCode`; filters active + not-deleted).
- Consumes: `ProductRepository.findByCategoryIdAndCode` (Task 1).

- [ ] **Step 1: Write the failing test for the core fix**

Add to `ProductDomainServiceTest.java` (mock `productRepository`, `categoryRepository`, `denomRepository`):
```java
@Test
void findOrCreateByBrand_sameBrandOtherCategory_createsNewRowForThisCategory() {
    UUID dataCatId = UUID.randomUUID();
    // TELKOMSEL exists in some OTHER category; must NOT be reused for dataCatId
    when(productRepository.findByCategoryIdAndCode(dataCatId, "TELKOMSEL"))
            .thenReturn(Optional.empty());
    when(productRepository.save(any(Products.class)))
            .thenAnswer(inv -> inv.getArgument(0));

    Products created = service.findOrCreateByBrand("TELKOMSEL", dataCatId);

    assertThat(created.getCode()).isEqualTo("TELKOMSEL");
    assertThat(created.getCategoryId()).isEqualTo(dataCatId);
    verify(productRepository).save(argThat(p -> p.getCategoryId().equals(dataCatId)));
}

@Test
void findByCategoryAndCode_resolvesCategoryThenLooksUp() {
    UUID catId = UUID.randomUUID();
    Category cat = new Category(); cat.setId(catId); cat.setCode("DATA");
    Products p = new Products(); p.setCode("TELKOMSEL"); p.setCategoryId(catId);
    p.setActive(true); p.setDeleted(false);
    when(categoryRepository.findByCode("DATA")).thenReturn(Optional.of(cat));
    when(productRepository.findByCategoryIdAndCode(catId, "TELKOMSEL")).thenReturn(Optional.of(p));

    assertThat(service.findByCategoryAndCode("DATA", "TELKOMSEL")).contains(p);
}
```

- [ ] **Step 2: Run the test, verify it fails**

Run: `mvn -pl satset-core test -Dtest=ProductDomainServiceTest`
Expected: FAIL — `findByCategoryAndCode` undefined and/or `findOrCreateByBrand` still calls global `findByCode`.

- [ ] **Step 3: Implement category-scoped lookup + fix**

In `ProductDomainService.java`:

Replace `findOrCreateByBrand` body's lookup (line 114) — change `productRepository.findByCode(code)` to `productRepository.findByCategoryIdAndCode(categoryId, code)`:
```java
    @Transactional
    @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager")
    public Products findOrCreateByBrand(String brand, UUID categoryId) {
        String code = CatalogCodeUtil.toCode(brand);
        return productRepository.findByCategoryIdAndCode(categoryId, code).map(existing -> {
            if (existing.isDeleted()) {
                existing.setDeleted(false);
                existing.setActive(true);
                return productRepository.save(existing);
            }
            return existing;
        }).orElseGet(() -> {
            Products p = new Products();
            p.setCode(code); p.setName(brand); p.setCategoryId(categoryId);
            p.setActive(true); p.setDeleted(false);
            return productRepository.save(p);
        });
    }
```

Add the storefront/sync lookup method (place near `findByCode`):
```java
    public Optional<Products> findByCategoryAndCode(String categoryCode, String code) {
        Optional<Category> category = categoryRepository.findByCode(categoryCode);
        if (category.isEmpty()) return Optional.empty();
        return productRepository.findByCategoryIdAndCode(category.get().getId(), code)
                .filter(p -> p.isActive() && !p.isDeleted());
    }
```

- [ ] **Step 4: Run the test, verify it passes**

Run: `mvn -pl satset-core test -Dtest=ProductDomainServiceTest`
Expected: PASS.

- [ ] **Step 5: Migrate the sync existence check + its test**

In `CatalogSyncService.previewProducts` (line ~102), replace the global existence check. Current:
```java
            if (seen.add(code) && productService.findByCode(code).isEmpty()) {
```
New (the `cat` Category is already in scope in this method):
```java
            if (seen.add(code) && productService.findByCategoryAndCode(cat.getCode(), code).isEmpty()) {
```

In `CatalogSyncServiceTest.java`, update the `previewProducts`/`applyProducts` stubs that used the global lookup. Replace occurrences of:
```java
        when(productService.findByCode("XL")).thenReturn(Optional.empty());
```
with:
```java
        when(productService.findByCategoryAndCode("PULSA", "XL")).thenReturn(Optional.empty());
```
(The `cat` in those tests has code `"PULSA"`; match each test's category code.)

- [ ] **Step 6: Run both suites, verify green**

Run: `mvn -pl satset-core test -Dtest=ProductDomainServiceTest,CatalogSyncServiceTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/service/ProductDomainService.java \
  satset-core/src/main/java/com/satset/supplier/service/CatalogSyncService.java \
  satset-core/src/test/java/com/satset/catalog/service/ProductDomainServiceTest.java \
  satset-core/src/test/java/com/satset/supplier/service/CatalogSyncServiceTest.java
git commit -m "fix(catalog): findOrCreateByBrand + sync preview are category-scoped

Core fix: a brand now gets its own row per category instead of the first
category claiming it globally.

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 3: Storefront product + denom endpoints nested under category

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/web/ProductCatalogController.java:70-85`
- Modify: `satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java:24-46`
- Modify: `satset-core/src/main/resources/templates/pages/purchase/index.html:319`
- Test: `satset-core/src/test/java/com/satset/catalog/web/ProductCatalogControllerTest.java`

**Interfaces:**
- Produces: `DenomDomainService.findByProduct(String categoryCode, String productCode) : List<ProductDenoms>` (category-scoped; injects `CategoryRepository`).
- Consumes: `ProductDomainService.findByCategoryAndCode` (Task 2), `ProductRepository.findByCategoryIdAndCode` (Task 1).
- Produces routes: `GET /api/categories/{catCode}/products/{prodCode}`, `GET /api/categories/{catCode}/products/{prodCode}/denoms`.

- [ ] **Step 1: Write the failing controller test**

In `ProductCatalogControllerTest.java`, add (MockMvc; match the existing test's setup style):
```java
@Test
void getProductByCategoryAndCode_returnsProduct() throws Exception {
    Products p = new Products(); p.setCode("TELKOMSEL"); p.setName("TELKOMSEL");
    when(browseProductsUseCase.findByCategoryAndCode("DATA", "TELKOMSEL"))
            .thenReturn(Optional.of(p));
    mockMvc.perform(get("/api/categories/DATA/products/TELKOMSEL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("TELKOMSEL"));
}

@Test
void getDenomsByCategoryAndProduct_returnsList() throws Exception {
    when(browseDenomsUseCase.findByProduct("DATA", "TELKOMSEL"))
            .thenReturn(List.of());
    mockMvc.perform(get("/api/categories/DATA/products/TELKOMSEL/denoms"))
            .andExpect(status().isOk());
}
```
Remove any existing test that asserts the bare `/api/products/{code}` or `/api/products/{code}/denoms` routes.

- [ ] **Step 2: Run the test, verify it fails**

Run: `mvn -pl satset-core test -Dtest=ProductCatalogControllerTest`
Expected: FAIL — 404 (routes not yet nested) / `findByProduct(String,String)` undefined.

- [ ] **Step 3: Make `DenomDomainService.findByProduct` category-scoped**

In `DenomDomainService.java`:
- Add `CategoryRepository` field + constructor param:
```java
    private final CategoryRepository categoryRepository;

    public DenomDomainService(DenomRepository denomRepository,
            DenomMetaRepository metaRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        this.denomRepository = denomRepository;
        this.metaRepository = metaRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }
```
- Replace `findByProduct(String productCode)` (line 40):
```java
    public List<ProductDenoms> findByProduct(String categoryCode, String productCode) {
        Optional<Category> category = categoryRepository.findByCode(categoryCode);
        if (category.isEmpty()) return List.of();
        Optional<Products> product =
                productRepository.findByCategoryIdAndCode(category.get().getId(), productCode);
        if (product.isEmpty()) return List.of();
        return denomRepository
                .findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(product.get().getId());
    }
```
Add imports: `com.satset.catalog.model.Category`, `com.satset.catalog.repository.CategoryRepository`.

- [ ] **Step 4: Nest the controller routes**

In `ProductCatalogController.java`, replace the two bare handlers (lines 70-85):
```java
    @GetMapping("/categories/{catCode}/products/{prodCode}")
    public ResponseEntity<ProductDTO> getProductByCategoryAndCode(
            @PathVariable String catCode, @PathVariable String prodCode) {
        return browseProductsUseCase.findByCategoryAndCode(catCode, prodCode)
                .map(CatalogDtoMapper::toProductDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories/{catCode}/products/{prodCode}/denoms")
    public ResponseEntity<List<ProductDenomDTO>> getDenomsByCategoryAndProduct(
            @PathVariable String catCode, @PathVariable String prodCode) {
        List<ProductDenomDTO> dtos = browseDenomsUseCase.findByProduct(catCode, prodCode).stream()
                .map(this::toDenomDTO).toList();
        return ResponseEntity.ok(dtos);
    }
```
(The bare `GET /products/{code}` and `GET /products/{code}/denoms` handlers are deleted.)

- [ ] **Step 5: Rewire the purchase page JS**

In `purchase/index.html`, line 319, replace:
```javascript
                            const r = await fetch(`/api/products/${p.code}/denoms`);
```
with (the selected category object is `this.activeCat`, set in `openCat`):
```javascript
                            const r = await fetch(`/api/categories/${this.activeCat.code}/products/${p.code}/denoms`);
```

- [ ] **Step 6: Run the controller test, verify it passes**

Run: `mvn -pl satset-core test -Dtest=ProductCatalogControllerTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/web/ProductCatalogController.java \
  satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java \
  satset-core/src/main/resources/templates/pages/purchase/index.html \
  satset-core/src/test/java/com/satset/catalog/web/ProductCatalogControllerTest.java
git commit -m "feat(catalog): storefront product/denom endpoints nested under category

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 4: Category-scoped CRUD dup-check, remove global methods, delete DataSeeder

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/service/ProductDomainService.java:53-56,73,96`
- Modify: `satset-core/src/main/java/com/satset/catalog/repository/ProductRepository.java`
- Delete: `satset-core/src/main/java/com/satset/DataSeeder.java`
- Test: `satset-core/src/test/java/com/satset/catalog/service/ProductDomainServiceTest.java`

**Interfaces:**
- Consumes: `ProductRepository.findByCategoryIdAndCode`, `existsByCategoryIdAndCodeAndIdNot` (Task 1).
- Removes: `ProductRepository.findByCode`, `existsByCodeAndIdNot`; `ProductDomainService.findByCode`.

- [ ] **Step 1: Write the failing test for category-scoped dup-check**

Add to `ProductDomainServiceTest.java`:
```java
@Test
void create_duplicateCodeInSameCategory_throws() {
    UUID catId = UUID.randomUUID();
    Category cat = new Category(); cat.setId(catId);
    when(categoryRepository.findById(catId)).thenReturn(Optional.of(cat));
    when(productRepository.findByCategoryIdAndCode(catId, "XL"))
            .thenReturn(Optional.of(new Products()));
    CreateProductRequest req = new CreateProductRequest(
            catId, "XL", "XL", null, null, null, true, 0);
    assertThatThrownBy(() -> service.create(req))
            .isInstanceOf(BusinessException.class);
}
```
(Match `CreateProductRequest`'s actual constructor arg order/count from its record definition.)

- [ ] **Step 2: Run the test, verify it fails**

Run: `mvn -pl satset-core test -Dtest=ProductDomainServiceTest`
Expected: FAIL — `create` still calls global `findByCode`, stub not matched → no exception thrown.

- [ ] **Step 3: Make CRUD dup-checks category-scoped + drop the global service method**

In `ProductDomainService.java`:
- `create` (line 73): replace
  `if (productRepository.findByCode(req.code().toUpperCase().trim()).isPresent()) {`
  with
  `if (productRepository.findByCategoryIdAndCode(category.getId(), req.code().toUpperCase().trim()).isPresent()) {`
- `update` (line 96): replace
  `if (productRepository.existsByCodeAndIdNot(req.code().toUpperCase().trim(), id)) {`
  with
  `if (productRepository.existsByCategoryIdAndCodeAndIdNot(category.getId(), req.code().toUpperCase().trim(), id)) {`
- Delete the now-unused global storefront method `findByCode` (lines 53-56).

- [ ] **Step 4: Delete DataSeeder and remove the global repo methods**

```bash
git rm satset-core/src/main/java/com/satset/DataSeeder.java
```
In `ProductRepository.java`, remove:
```java
    Optional<Products> findByCode(String code);
```
and
```java
    boolean existsByCodeAndIdNot(String code, UUID id);
```

- [ ] **Step 5: Compile-check for stragglers**

Run: `mvn -pl satset-core test-compile`
Expected: BUILD SUCCESS. If any `findByCode`/`existsByCodeAndIdNot` caller on `Products` remains, the compile error names it — migrate it to the category-scoped equivalent, then re-run.

- [ ] **Step 6: Run the full suite, verify green**

Run: `mvn -pl satset-core test`
Expected: BUILD SUCCESS, 0 failures / 0 errors.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(catalog): category-scoped CRUD dup-check, drop global code lookups, rm DataSeeder

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 5: Live verification

**Files:** none (runtime check).

- [ ] **Step 1: Rebuild + run the app**

Run: `mvn -pl satset-core spring-boot:run` (in a background shell; app on :8080).

- [ ] **Step 2: Wipe dev catalog + full sync**

Wipe:
```bash
docker exec -i postgres-satset psql -U admin -d satset_go -c "
DELETE FROM product_denoms; DELETE FROM products; DELETE FROM categories;"
```
From the logged-in admin browser console:
```javascript
fetch('/api/admin/catalog/sync/all',{method:'POST',headers:{'X-XSRF-TOKEN':document.cookie.match(/XSRF-TOKEN=([^;]+)/)[1]}}).then(r=>r.json()).then(console.log)
```
Expected: `SyncResult` JSON with `added > 0`, `failed = 0`.

- [ ] **Step 3: Verify products exist per category, brand repeats across categories**

```bash
docker exec -i postgres-satset psql -U admin -d satset_go -c "
SELECT c.name, count(p.id) prod FROM categories c
LEFT JOIN products p ON p.category_id = c.id GROUP BY c.name ORDER BY prod;"
```
Expected: Voucher / Data / Aktivasi Voucher have products (> 0), none at 0 that DF populates.
```bash
docker exec -i postgres-satset psql -U admin -d satset_go -c "
SELECT c.name, p.code FROM products p JOIN categories c ON c.id=p.category_id
WHERE p.code='TELKOMSEL' ORDER BY c.name;"
```
Expected: TELKOMSEL appears as a distinct row under each category DF lists it in (e.g. Pulsa + Data).

- [ ] **Step 4: Verify storefront**

Open `/purchase`, pick Data → TELKOMSEL → denoms load (nested endpoint). Then pick Pulsa → TELKOMSEL → its denoms load independently.

---

## Prod migration (required before deploy, not a code task)

`ddl-auto=validate` does not check unique constraints; run manually against prod:
```sql
ALTER TABLE products DROP CONSTRAINT <old UNIQUE(code) name>;  -- find via \d products
ALTER TABLE products ADD  CONSTRAINT uq_products_category_code UNIQUE (category_id, code);
```

## Self-Review

- **Spec coverage:** entity constraint (T1), repo category-scoped queries (T1), `findOrCreateByBrand` core fix (T2), sync preview (T2), storefront nested routes + denom scope (T3), purchase JS rewire (T3), CRUD dup-check (T4), remove global methods (T4), delete DataSeeder (T4), dev migration (T1 step 1), prod migration (documented), live verification (T5). All spec sections mapped.
- **Placeholders:** `<old>` constraint name in T1/prod is resolved at runtime via `\d products` (documented, not a gap). `CreateProductRequest` constructor args flagged to match the record — implementer confirms at T4 step 1.
- **Type consistency:** `findByCategoryIdAndCode(UUID,String)`, `existsByCategoryIdAndCodeAndIdNot(UUID,String,UUID)`, `findByCategoryAndCode(String,String)`, `findByProduct(String,String)` used consistently across tasks.
