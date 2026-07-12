# All-Denoms Tab + Markup Pricing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Halaman "Semua Denom" lintas produk di admin katalog dengan markup engine (persen/fix, dari modal atau harga sekarang, ceil kelipatan 100) yang reuse confirm-modal + endpoint bulk price dari fitur inline-edit.

**Architecture:** Backend nambah satu read endpoint (`GET /api/admin/catalog/denoms`) + DTO enriched (categoryName/productName), simpan tetap lewat `PUT /denoms/prices` existing. Frontend: logika price-edit di-extract ke shared JS factory (dipakai `denoms.html` existing + `all-denoms.html` baru), confirm-modal HTML di-fragment-kan. Markup engine murni client, isi `dirty` map yang sama.

**Tech Stack:** Spring Boot 4, Java 25, JUnit 5 + AssertJ + Mockito (standalone MockMvc), Thymeleaf + Alpine.js + Tailwind/DaisyUI.

**Spec:** `docs/superpowers/specs/2026-07-12-all-denoms-tab-markup-pricing-design.md`

## Global Constraints

- SKU (`code`) read-only — DF-owned, tidak editable di mana pun.
- JANGAN expose `e.getMessage()` ke client.
- Markup rounding: `Math.ceil(raw / 100) * 100`.
- Base markup: `MODAL` → `basePrice`; `CURRENT` → `price ?? basePrice`. `base == null` → skip baris.
- Simpan harga: HANYA lewat `PUT /api/admin/catalog/denoms/prices` (jangan bikin endpoint tulis harga baru).
- Read endpoint guard `REALM_view_catalog`; halaman guard sama (class-level di `AdminCatalogPageController`).
- TDD strict backend. Semua command jalan dari worktree root: `/Users/kokohbudi/myProjects/satset-go/.claude/worktrees/live-edit-product-denom`.
- `git rm` untuk hapus/rename file (bukan `rm`).

---

### Task 1: Backend — global denom list endpoint

**Files:**
- Create: `satset-core/src/main/java/com/satset/catalog/dto/DenomListItemDTO.java`
- Modify: `satset-core/src/main/java/com/satset/catalog/repository/DenomRepository.java`
- Modify: `satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java`
- Modify: `satset-core/src/main/java/com/satset/catalog/web/CatalogDtoMapper.java`
- Modify: `satset-core/src/main/java/com/satset/catalog/web/AdminCatalogController.java`
- Test: `satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java`, `satset-core/src/test/java/com/satset/catalog/web/AdminCatalogControllerTest.java`

**Interfaces:**
- Produces: `DenomDomainService.findAllForList()` → `List<DenomListItemDTO>` (enriched, non-deleted, urut productId+sortOrder); `GET /api/admin/catalog/denoms` returns that. Task 3 SSR-seeds & Task 4 fetch-nya.

- [ ] **Step 1: DTO record**

`DenomListItemDTO.java`:

```java
package com.satset.catalog.dto;

import com.satset.catalog.model.DenomType;

import java.math.BigDecimal;
import java.util.UUID;

/** Baris tabel "Semua Denom" — denom + konteks produk/kategori untuk tampilan lintas produk. */
public record DenomListItemDTO(
    UUID id,
    String code,
    String name,
    DenomType denomType,
    BigDecimal nominal,
    BigDecimal price,
    BigDecimal basePrice,
    boolean active,
    boolean deleted,
    UUID productId,
    String productName,
    String categoryName
) {}
```

- [ ] **Step 2: Repo query**

Tambah di `DenomRepository`:

```java
    List<ProductDenoms> findByDeletedFalseOrderByProductIdAscSortOrderAsc();
```

- [ ] **Step 3: Failing service test**

Tambah di `DenomDomainServiceTest` (imports: `import com.satset.catalog.dto.DenomListItemDTO;`). Di `setUp`, `product` sudah punya `categoryId`? Set eksplisit di test ini. Test:

```java
    @Test
    void findAllForList_EnrichesProductAndCategoryName_NoNPlusOne() {
        product.setCategoryId(categoryId);
        ProductDenoms d2 = new ProductDenoms();
        d2.setId(UUID.randomUUID());
        d2.setProductId(productId);
        d2.setCode("TLKM10");
        d2.setName("Telkomsel 10K");
        d2.setDenomType(DenomType.FIXED_DENOM);
        d2.setPrice(new BigDecimal("10500"));
        d2.setBasePrice(new BigDecimal("10000"));

        when(denomRepository.findByDeletedFalseOrderByProductIdAscSortOrderAsc())
                .thenReturn(List.of(existingDenom, d2));
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(categoryRepository.findAll()).thenReturn(List.of(category));

        List<DenomListItemDTO> result = denomService.findAllForList();

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(item -> {
            assertThat(item.productName()).isEqualTo("Telkomsel");
            assertThat(item.categoryName()).isEqualTo("PULSA");
        });
        assertThat(result.get(1).code()).isEqualTo("TLKM10");
        assertThat(result.get(1).basePrice()).isEqualByComparingTo("10000");
        // enrichment sekali fetch — bukan per denom
        verify(productRepository, times(1)).findAll();
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    void findAllForList_UnknownProduct_NullNames() {
        when(denomRepository.findByDeletedFalseOrderByProductIdAscSortOrderAsc())
                .thenReturn(List.of(existingDenom));
        when(productRepository.findAll()).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<DenomListItemDTO> result = denomService.findAllForList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productName()).isNull();
        assertThat(result.get(0).categoryName()).isNull();
    }
```

Catatan: `category.setName(...)` belum di-set di `setUp` (cuma `setCode("PULSA")`). Enrichment pakai `categoryName` = `Category.getName()`. Di test set `category.setName("PULSA")` di dalam test pertama sebelum stubbing, ATAU ganti assertion ke code. **Keputusan**: enrichment pakai `getName()`; tambahkan `category.setName("PULSA");` di awal test pertama.

- [ ] **Step 4: Run test, verify FAIL**

```bash
cd /Users/kokohbudi/myProjects/satset-go/.claude/worktrees/live-edit-product-denom
mvn -q -pl satset-core test -Dtest=DenomDomainServiceTest 2>&1 | tail -20
```

Expected: COMPILATION ERROR / FAIL — `findAllForList` belum ada.

- [ ] **Step 5: Service impl**

Di `DenomDomainService`, imports:

```java
import com.satset.catalog.dto.DenomListItemDTO;
import com.satset.catalog.web.CatalogDtoMapper;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
```

Method (di section browse/read, mis. setelah `findByProductForAdmin`):

```java
    public List<DenomListItemDTO> findAllForList() {
        Map<UUID, Products> productById = productRepository.findAll().stream()
                .collect(Collectors.toMap(Products::getId, Function.identity()));
        Map<UUID, String> categoryNameById = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return denomRepository.findByDeletedFalseOrderByProductIdAscSortOrderAsc().stream()
                .map(d -> {
                    Products p = productById.get(d.getProductId());
                    String productName = p != null ? p.getName() : null;
                    String categoryName = (p != null) ? categoryNameById.get(p.getCategoryId()) : null;
                    return CatalogDtoMapper.toDenomListItemDTO(d, productName, categoryName);
                })
                .toList();
    }
```

- [ ] **Step 6: Mapper method**

Di `CatalogDtoMapper`, import `com.satset.catalog.dto.DenomListItemDTO;`, tambah:

```java
    static DenomListItemDTO toDenomListItemDTO(ProductDenoms d, String productName, String categoryName) {
        return new DenomListItemDTO(
                d.getId(), d.getCode(), d.getName(), d.getDenomType(), d.getNominal(),
                d.getPrice(), d.getBasePrice(), d.isActive(), d.isDeleted(),
                d.getProductId(), productName, categoryName);
    }
```

- [ ] **Step 7: Run service test, verify PASS**

```bash
mvn -q -pl satset-core test -Dtest=DenomDomainServiceTest 2>&1 | tail -20
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Failing controller test**

Di `AdminCatalogControllerTest` (import `com.satset.catalog.dto.DenomListItemDTO;`, `com.satset.catalog.model.DenomType;`):

```java
    @Test
    void listAllDenoms_ReturnsEnrichedDTOs() throws Exception {
        UUID id = UUID.randomUUID();
        UUID prodId = UUID.randomUUID();
        when(manageDenomsUseCase.findAllForList()).thenReturn(List.of(
                new DenomListItemDTO(id, "byu10", "by.U 10K", DenomType.FIXED_DENOM,
                        null, new java.math.BigDecimal("10500"), new java.math.BigDecimal("10000"),
                        true, false, prodId, "by.U", "PULSA")));

        mockMvc.perform(get("/api/admin/catalog/denoms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("byu10"))
                .andExpect(jsonPath("$[0].productName").value("by.U"))
                .andExpect(jsonPath("$[0].categoryName").value("PULSA"));
    }
```

- [ ] **Step 9: Run, verify FAIL** (`mvn -q -pl satset-core test -Dtest=AdminCatalogControllerTest 2>&1 | tail -15` → 404).

- [ ] **Step 10: Controller endpoint**

Di `AdminCatalogController`, import `com.satset.catalog.dto.DenomListItemDTO;`. Tambah di section Denoms (sebelum `listDenoms` per-product, atau setelahnya):

```java
    @GetMapping("/denoms")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<DenomListItemDTO>> listAllDenoms() {
        return ResponseEntity.ok(manageDenomsUseCase.findAllForList());
    }
```

Guard rute: `/denoms` (literal, no path var) tidak bentrok dengan `/products/{productId}/denoms` maupun `/denoms/{id}` (path beda segмент). Aman.

- [ ] **Step 11: Run both test classes, verify PASS**

```bash
mvn -q -pl satset-core test -Dtest=DenomDomainServiceTest,AdminCatalogControllerTest 2>&1 | tail -15
```

Expected: BUILD SUCCESS.

- [ ] **Step 12: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/dto/DenomListItemDTO.java \
        satset-core/src/main/java/com/satset/catalog/repository/DenomRepository.java \
        satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java \
        satset-core/src/main/java/com/satset/catalog/web/CatalogDtoMapper.java \
        satset-core/src/main/java/com/satset/catalog/web/AdminCatalogController.java \
        satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java \
        satset-core/src/test/java/com/satset/catalog/web/AdminCatalogControllerTest.java
git commit -m "feat(catalog): global denom list endpoint with product/category context

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: Extract shared price-edit JS factory + confirm-modal fragment (refactor denoms.html)

Pure refactor — TIDAK mengubah perilaku `denoms.html`. Dilakukan sebelum halaman baru supaya kedua halaman pakai satu sumber logika.

**Files:**
- Create: `satset-core/src/main/resources/static/js/denom-price-editing.js`
- Create: `satset-core/src/main/resources/templates/fragments/denom-price-confirm-modal.html`
- Modify: `satset-core/src/main/resources/templates/pages/admin/catalog/denoms.html`

**Interfaces:**
- Produces: global `window.denomPriceEditing(getDenoms)` — factory balikin objek dengan: state `dirty`, `showPriceModal`, `savingPrices`, `priceResults`, `filterUnpriced`; getter `dirtyList`, `unpricedCount`; methods `isDirty(d)`, `pendingPrice(d)`, `resultFor(id)`, `setPrice(d,val)`, `openPriceModal()`, `submitPrices()`. `getDenoms` = fungsi balikin array denom terbaru (buat `unpricedCount` & reload). Thymeleaf fragment `denom-price-confirm-modal :: modal` = markup confirm modal (pakai `dirtyList`, `priceResults`, `formatRp`, `resultFor`, dari komponen host). Task 4 pakai dua-duanya.

- [ ] **Step 1: Baca denoms.html bagian price-edit**

Baca `satset-core/src/main/resources/templates/pages/admin/catalog/denoms.html`. Identifikasi blok yang ditambah fitur inline-edit:
- State: `dirty`, `showPriceModal`, `savingPrices`, `priceResults`, `filterUnpriced`.
- Getter/method: `dirtyList`, `unpricedCount`, `isDirty`, `pendingPrice`, `resultFor`, `setPrice`, `openPriceModal`, `submitPrices`, dan guard auto-reset di getter `filteredDenoms`.
- Confirm modal markup (`<!-- Bulk Price Confirm Modal -->`).

- [ ] **Step 2: Tulis shared factory**

`satset-core/src/main/resources/static/js/denom-price-editing.js`:

```javascript
/**
 * Shared price-editing mixin untuk halaman denom (per-produk & semua-denom).
 * Host component harus punya: getDenoms() (via arg), formatRp(v), loadDenoms(),
 * dan Alpine store 'toast'. Spread hasil factory ke return object komponen.
 */
window.denomPriceEditing = function (getDenoms) {
    return {
        dirty: {},
        showPriceModal: false,
        savingPrices: false,
        priceResults: [],
        filterUnpriced: false,

        get dirtyList() { return Object.values(this.dirty); },
        get unpricedCount() {
            return getDenoms().filter(d => !d.deleted && d.price == null).length;
        },
        isDirty(d) { return !!this.dirty[d.id]; },
        pendingPrice(d) { return this.dirty[d.id] ? this.dirty[d.id].newPrice : d.price; },
        resultFor(id) { return this.priceResults.find(r => r.id === id); },

        setPrice(d, val) {
            const p = val === '' || val == null ? null : Number(val);
            const unchanged = p === null || (d.price != null && Number(d.price) === p);
            if (unchanged) {
                delete this.dirty[d.id];
            } else {
                this.dirty[d.id] = { id: d.id, code: d.code, name: d.name, oldPrice: d.price, newPrice: p };
            }
        },

        openPriceModal() {
            this.priceResults = [];
            this.showPriceModal = true;
        },

        async submitPrices() {
            this.savingPrices = true;
            try {
                const res = await fetch('/api/admin/catalog/denoms/prices', {
                    method: 'PUT', headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(this.dirtyList.map(x => ({ id: x.id, price: x.newPrice })))
                });
                if (!res.ok) throw new Error('Gagal menyimpan harga');
                this.priceResults = await res.json();
                const okCount = this.priceResults.filter(r => r.ok).length;
                const failCount = this.priceResults.length - okCount;
                for (const r of this.priceResults) {
                    if (r.ok) delete this.dirty[r.id];
                }
                await this.loadDenoms();
                if (failCount === 0) {
                    this.showPriceModal = false;
                    Alpine.store('toast').success(`${okCount} harga diperbarui`);
                } else {
                    Alpine.store('toast').error(`${failCount} gagal disimpan, ${okCount} sukses`);
                }
            } catch (e) {
                Alpine.store('toast').error(e.message);
            } finally {
                this.savingPrices = false;
            }
        }
    };
};
```

- [ ] **Step 3: Tulis confirm-modal fragment**

`satset-core/src/main/resources/templates/fragments/denom-price-confirm-modal.html` — pindahkan markup `<!-- Bulk Price Confirm Modal -->` dari denoms.html apa adanya, bungkus fragment:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<div th:fragment="modal" class="modal" :class="{ 'modal-open': showPriceModal }">
    <div class="modal-box max-w-xl">
        <h3 class="text-lg font-semibold mb-4">Konfirmasi Perubahan Harga</h3>
        <div class="overflow-x-auto max-h-80">
            <table class="table table-sm">
                <thead>
                <tr>
                    <th>Code</th>
                    <th>Nama</th>
                    <th class="text-right">Harga Lama</th>
                    <th class="text-right">Harga Baru</th>
                    <th class="text-center" x-show="priceResults.length">Hasil</th>
                </tr>
                </thead>
                <tbody>
                <template x-for="item in dirtyList" :key="item.id">
                    <tr>
                        <td class="font-mono text-xs" x-text="item.code"></td>
                        <td x-text="item.name"></td>
                        <td class="text-right font-mono" x-text="formatRp(item.oldPrice)"></td>
                        <td class="text-right font-mono font-semibold" x-text="formatRp(item.newPrice)"></td>
                        <td class="text-center" x-show="priceResults.length">
                            <template x-if="resultFor(item.id)">
                                <span class="badge badge-sm"
                                      :class="resultFor(item.id).ok ? 'badge-success' : 'badge-error'"
                                      :title="resultFor(item.id).error || ''"
                                      x-text="resultFor(item.id).ok ? 'OK' : (resultFor(item.id).error || 'Gagal')"></span>
                            </template>
                        </td>
                    </tr>
                </template>
                </tbody>
            </table>
        </div>
        <div class="modal-action">
            <button class="btn btn-ghost" @click="showPriceModal = false" :disabled="savingPrices">Batal</button>
            <button class="btn btn-warning" @click="submitPrices()" :disabled="savingPrices">
                <span x-show="savingPrices" class="loading loading-spinner loading-xs"></span>
                Konfirmasi
            </button>
        </div>
    </div>
</div>
</body>
</html>
```

- [ ] **Step 4: Rewire denoms.html**

Di `denoms.html`:
1. Hapus state+method price-edit dari `denomManager()` (yang sekarang ada di factory). Ganti dengan spread di return object:

```js
    function denomManager() {
        return {
            ...window.denomPriceEditing(() => this.denoms),
            // ... sisa state/method existing (canManage, productId, denoms, loadDenoms, dst.)
```

PENTING: `getDenoms` = `() => this.denoms` — arrow supaya `this` = komponen Alpine saat dipanggil. Karena spread mengevaluasi factory sebelum komponen jadi, `getDenoms` harus lazy (fungsi), bukan `this.denoms` langsung. Factory sudah lazy (`getDenoms()` dipanggil di dalam getter). OK.

2. `filteredDenoms` getter: pertahankan guard auto-reset + filter unpriced (butuh `this.filterUnpriced` & `this.unpricedCount` — dua-duanya dari factory, tetap kebaca via `this`).
3. Ganti markup confirm modal dengan `<div th:replace="~{fragments/denom-price-confirm-modal :: modal}"></div>`.
4. Tambah `<script defer th:src="@{/js/denom-price-editing.js}"></script>` di `layout:fragment="scripts"` block (cek base.html — `scripts` fragment ada di L424). Pastikan load SEBELUM inline `denomManager()` dieksekusi: taruh script src di awal block scripts, `defer` + inline script tanpa defer → inline jalan setelah DOM tapi `denomPriceEditing` global sudah ada karena `defer` script dievaluasi sebelum `DOMContentLoaded`. Aman selama inline `denomManager` dipanggil via Alpine init (setelah DOMContentLoaded).

- [ ] **Step 5: Verifikasi regresi (backend build + manual)**

```bash
mvn -q -pl satset-core test -Dtest=DenomDomainServiceTest,AdminCatalogControllerTest 2>&1 | tail -8
```

Expected: BUILD SUCCESS (template tak dicompile — sanity). Manual (di Step verifikasi browser Task 4, gabung): buka halaman denom per-produk, pastikan inline edit + confirm + banner masih jalan identik.

- [ ] **Step 6: Commit**

```bash
git add satset-core/src/main/resources/static/js/denom-price-editing.js \
        satset-core/src/main/resources/templates/fragments/denom-price-confirm-modal.html \
        satset-core/src/main/resources/templates/pages/admin/catalog/denoms.html
git commit -m "refactor(catalog): extract shared denom price-editing JS + confirm-modal fragment

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: Page route + SSR + nav tabs

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/web/AdminCatalogPageController.java`
- Modify: `satset-core/src/main/resources/templates/pages/admin/catalog/categories.html`
- Modify: `satset-core/src/main/resources/templates/pages/admin/catalog/products.html`

**Interfaces:**
- Consumes: `DenomDomainService.findAllForList()` (Task 1), `CategoryDomainService.findAllForAdmin()`.
- Produces: route `GET /admin/catalog/denoms` → model attrs `initialDenoms` (List<DenomListItemDTO>), `initialCategories` (List<CategoryDTO>) → template `pages/admin/catalog/all-denoms` (dibuat Task 4).

- [ ] **Step 1: Page route**

Di `AdminCatalogPageController`, tambah import `com.satset.catalog.dto.DenomListItemDTO;`. Method:

```java
    @GetMapping("/denoms")
    public String allDenomsPage(Model model) {
        model.addAttribute("currentPage", "admin-catalog");
        model.addAttribute("breadcrumb", "Semua Denom");

        List<CategoryDTO> categories = manageCategoriesUseCase.findAllForAdmin().stream()
                .map(CatalogDtoMapper::toCategoryDTO).toList();
        model.addAttribute("initialCategories", categories);

        List<DenomListItemDTO> denoms = manageDenomsUseCase.findAllForList();
        model.addAttribute("initialDenoms", denoms);

        return "pages/admin/catalog/all-denoms";
    }
```

- [ ] **Step 2: Nav tab di categories.html + products.html**

Di tab list kedua file (`<div role="tablist" class="tabs tabs-bordered ...">`), tambah setelah tab "Semua Produk":

```html
            <a role="tab" class="tab" href="/admin/catalog/denoms">Semua Denom</a>
```

(categories.html: cari tab list serupa; kalau belum ada tab nav, tambahkan konsisten dengan products.html. Verifikasi dengan membaca kedua file dulu.)

- [ ] **Step 3: Verifikasi build**

```bash
mvn -q -pl satset-core test -Dtest=AdminCatalogControllerTest 2>&1 | tail -6
```

Expected: BUILD SUCCESS (route tak ada test unit; template all-denoms belum ada tapi tak dicompile saat test).

- [ ] **Step 4: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/web/AdminCatalogPageController.java \
        satset-core/src/main/resources/templates/pages/admin/catalog/categories.html \
        satset-core/src/main/resources/templates/pages/admin/catalog/products.html
git commit -m "feat(catalog): all-denoms page route + SSR seed + nav tab

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: all-denoms.html — tabel, filter, checkbox, markup bar

**Files:**
- Create: `satset-core/src/main/resources/templates/pages/admin/catalog/all-denoms.html`

**Interfaces:**
- Consumes: shared `window.denomPriceEditing` + fragment `denom-price-confirm-modal :: modal` (Task 2); SSR `initialDenoms`, `initialCategories` (Task 3); endpoint `GET /api/admin/catalog/denoms` + `PUT /denoms/prices`.
- Produces: UI final.

- [ ] **Step 1: Template shell + SSR seed**

Buat `all-denoms.html` decorate `layouts/base`, `x-data="allDenomManager()"`. SSR seed pola existing (lihat denoms.html L279+ `PRODUCT_ID`/`INITIAL_DENOMS`). Inline script sebelum `<script defer src=...>`:

```html
<th:block layout:fragment="scripts">
    <script th:inline="javascript">
        const INITIAL_DENOMS = /*[[${initialDenoms}]]*/ [];
        const INITIAL_CATEGORIES = /*[[${initialCategories}]]*/ [];
        const CAN_MANAGE = /*[[${#authorization.expression('hasRole(''REALM_manage_denoms'')')}]]*/ false;
    </script>
    <script defer th:src="@{/js/denom-price-editing.js}"></script>
    <script>
        /* allDenomManager() inline di sini (Step 4) */
    </script>
</th:block>
```

(Ikuti pola script existing denoms.html — inline `allDenomManager()` di `<script>` biasa dalam `th:block`. `denom-price-editing.js` `defer` load sebelum inline dieksekusi via Alpine init.)

- [ ] **Step 2: Tab nav + header + markup bar markup**

Tab nav (aktif "Semua Denom"):

```html
    <div role="tablist" class="tabs tabs-bordered mb-6 animate-rise" style="--i:0">
        <a role="tab" class="tab" href="/admin/catalog/categories">Kategori</a>
        <a role="tab" class="tab" href="/admin/catalog/products">Semua Produk</a>
        <a role="tab" class="tab tab-active">Semua Denom</a>
    </div>
```

Markup bar (hanya `canManage`):

```html
    <div sec:authorize="hasRole('REALM_manage_denoms')" class="card bg-base-100 shadow-sm p-4 mb-4 animate-rise" style="--i:1">
        <div class="flex flex-wrap items-end gap-3">
            <label class="form-control">
                <span class="label-text text-xs">Base</span>
                <select class="select select-bordered select-sm" x-model="markup.base">
                    <option value="MODAL">Harga Modal</option>
                    <option value="CURRENT">Harga Sekarang</option>
                </select>
            </label>
            <label class="form-control">
                <span class="label-text text-xs">Tipe</span>
                <select class="select select-bordered select-sm" x-model="markup.type">
                    <option value="PERSEN">Persen (%)</option>
                    <option value="FIX">Fix (Rp)</option>
                </select>
            </label>
            <label class="form-control">
                <span class="label-text text-xs">Nilai</span>
                <input type="number" min="0" step="any" class="input input-bordered input-sm w-32" x-model.number="markup.value"/>
            </label>
            <button class="btn btn-primary btn-sm tap" @click="applyMarkup()"
                    :disabled="selectedIds.length === 0 || markup.value === null || markup.value === ''">
                Terapkan ke terpilih (<span x-text="selectedIds.length"></span>)
            </button>
            <button x-show="dirtyList.length > 0" x-cloak class="btn btn-warning btn-sm tap" @click="openPriceModal()">
                Simpan Harga (<span x-text="dirtyList.length"></span>)
            </button>
        </div>
    </div>
```

- [ ] **Step 3: Filter bar + tabel**

Filter (kategori dropdown + search) + tabel dengan checkbox header (pilih semua tampil) + kolom Kategori/Produk/Code/Nama/Nominal/Modal/Harga Jual(input)/Status. Reuse pola price cell dari denoms.html (input `:value="pendingPrice(d)"` `@change="setPrice(d, $event.target.value)"` `:class="isDirty(d) ? 'input-warning' : ''"`). Banner unpriced + confirm modal fragment:

```html
    <!-- unpriced banner -->
    <div x-show="unpricedCount > 0" x-cloak class="alert alert-warning py-2 mb-4 flex items-center justify-between">
        <span>⚠ <b x-text="unpricedCount"></b> denom belum ada harga jual</span>
        <button class="btn btn-xs tap" @click="filterUnpriced = !filterUnpriced"
                x-text="filterUnpriced ? 'Tampilkan semua' : 'Tampilkan'"></button>
    </div>

    <!-- filter -->
    <div class="flex flex-wrap gap-3 mb-4 animate-rise" style="--i:2">
        <select class="select select-bordered select-sm" x-model="filterCategory">
            <option value="">Semua Kategori</option>
            <template x-for="c in categories" :key="c.id"><option :value="c.name" x-text="c.name"></option></template>
        </select>
        <input type="search" x-model="searchQuery" placeholder="Cari code/nama/produk..." class="input input-bordered input-sm grow max-w-xs"/>
    </div>

    <!-- table -->
    <div class="card bg-base-100 shadow-sm animate-rise" style="--i:3">
        <div class="overflow-x-auto">
            <table class="table table-zebra table-sm">
                <thead>
                <tr>
                    <th sec:authorize="hasRole('REALM_manage_denoms')">
                        <input type="checkbox" class="checkbox checkbox-sm" @change="toggleSelectAll($event.target.checked)" :checked="allVisibleSelected()"/>
                    </th>
                    <th>Kategori</th><th>Produk</th><th>Code</th><th>Nama</th>
                    <th class="text-right">Nominal</th><th class="text-right">Modal</th>
                    <th class="text-right">Harga Jual</th><th class="text-center">Status</th>
                </tr>
                </thead>
                <tbody>
                <template x-for="d in filteredDenoms" :key="d.id">
                    <tr :class="d.deleted ? 'opacity-40' : ''">
                        <td sec:authorize="hasRole('REALM_manage_denoms')">
                            <input type="checkbox" class="checkbox checkbox-sm" :value="d.id"
                                   x-model="selectedIds" :disabled="d.deleted"/>
                        </td>
                        <td x-text="d.categoryName || '-'"></td>
                        <td x-text="d.productName || '-'"></td>
                        <td class="font-mono text-xs" x-text="d.code"></td>
                        <td x-text="d.name"></td>
                        <td class="text-right font-mono" x-text="formatRp(d.nominal)"></td>
                        <td class="text-right font-mono" x-text="formatRp(d.basePrice)"></td>
                        <td class="text-right">
                            <template x-if="canManage && !d.deleted">
                                <input type="number" min="1" step="any"
                                       class="input input-bordered input-xs w-28 text-right font-mono"
                                       :class="isDirty(d) ? 'input-warning' : ''"
                                       :value="pendingPrice(d)"
                                       @change="setPrice(d, $event.target.value)"/>
                            </template>
                            <template x-if="!canManage || d.deleted">
                                <span class="font-mono" x-text="formatRp(d.price)"></span>
                            </template>
                        </td>
                        <td class="text-center">
                            <span class="badge badge-sm" :class="d.deleted ? 'badge-error' : (d.active ? 'badge-success' : 'badge-warning')"
                                  x-text="d.deleted ? 'Deleted' : (d.active ? 'Active' : 'Inactive')"></span>
                        </td>
                    </tr>
                </template>
                <tr x-show="filteredDenoms.length === 0"><td colspan="9" class="text-center py-8 text-base-content/50">Tidak ada denom</td></tr>
                </tbody>
            </table>
        </div>
    </div>

    <div th:replace="~{fragments/denom-price-confirm-modal :: modal}"></div>
```

- [ ] **Step 4: allDenomManager() component**

Inline script:

```js
    function allDenomManager() {
        return {
            ...window.denomPriceEditing(() => this.denoms),
            canManage: CAN_MANAGE,
            denoms: INITIAL_DENOMS,
            categories: INITIAL_CATEGORIES,
            searchQuery: '',
            filterCategory: '',
            selectedIds: [],
            markup: { base: 'MODAL', type: 'PERSEN', value: null },

            get filteredDenoms() {
                if (this.filterUnpriced && this.unpricedCount === 0) this.filterUnpriced = false;
                let list = this.denoms;
                if (this.filterUnpriced) list = list.filter(d => !d.deleted && d.price == null);
                if (this.filterCategory) list = list.filter(d => d.categoryName === this.filterCategory);
                if (this.searchQuery) {
                    const q = this.searchQuery.toLowerCase();
                    list = list.filter(d =>
                        d.code.toLowerCase().includes(q) ||
                        (d.name && d.name.toLowerCase().includes(q)) ||
                        (d.productName && d.productName.toLowerCase().includes(q)));
                }
                return list;
            },

            allVisibleSelected() {
                const vis = this.filteredDenoms.filter(d => !d.deleted);
                return vis.length > 0 && vis.every(d => this.selectedIds.includes(d.id));
            },
            toggleSelectAll(checked) {
                const visIds = this.filteredDenoms.filter(d => !d.deleted).map(d => d.id);
                if (checked) {
                    this.selectedIds = [...new Set([...this.selectedIds, ...visIds])];
                } else {
                    this.selectedIds = this.selectedIds.filter(id => !visIds.includes(id));
                }
            },

            applyMarkup() {
                const v = Number(this.markup.value);
                if (!(v >= 0)) { Alpine.store('toast').error('Nilai markup tidak valid'); return; }
                let skipped = 0, applied = 0;
                for (const id of this.selectedIds) {
                    const d = this.denoms.find(x => x.id === id);
                    if (!d || d.deleted) continue;
                    const base = this.markup.base === 'MODAL' ? d.basePrice : (d.price ?? d.basePrice);
                    if (base == null) { skipped++; continue; }
                    const raw = this.markup.type === 'PERSEN' ? Number(base) * (1 + v / 100) : Number(base) + v;
                    const newPrice = Math.ceil(raw / 100) * 100;
                    this.setPrice(d, newPrice);   // isi dirty map (shared)
                    applied++;
                }
                if (skipped > 0) Alpine.store('toast').error(`${applied} di-set, ${skipped} dilewati (tak ada modal/harga)`);
                else Alpine.store('toast').success(`${applied} harga di-set, cek & simpan`);
            },

            async loadDenoms() {
                try {
                    const res = await fetch('/api/admin/catalog/denoms');
                    this.denoms = await res.json();
                    this.selectedIds = [];
                } catch (e) {
                    Alpine.store('toast').error('Gagal memuat denom');
                }
            },

            formatRp(v) {
                if (v == null) return '-';
                return 'Rp ' + new Intl.NumberFormat('id-ID').format(v);
            }
        };
    }
```

Catatan self-check markup (ganti test unit): sebelum commit, di console browser / atau nalar manual, verifikasi 3 kasus:
- Persen: base 9950, v=5 → 9950*1.05=10447.5 → ceil/100*100 = **10500**.
- Fix: base 10000, v=250 → 10250 → **10300** (ceil kelipatan 100).
- Fallback: base=CURRENT tapi price null, basePrice 10000 → base=10000; kalau dua-duanya null → skipped++.

- [ ] **Step 5: Verifikasi build**

```bash
mvn -q -pl satset-core test -Dtest=DenomDomainServiceTest,AdminCatalogControllerTest 2>&1 | tail -6
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Verifikasi manual browser (gabung regresi Task 2)**

Jalankan app (dev), login admin `manage_denoms`:
1. `/admin/catalog/products` → klik tab "Semua Denom" → tabel semua denom lintas produk tampil, kolom Kategori/Produk keisi.
2. Filter kategori + search → tabel menyusut benar.
3. Centang beberapa baris (atau checkbox header "pilih semua tampil") → counter tombol Terapkan naik.
4. Base=Modal, Tipe=Persen, Nilai=5 → Terapkan → harga jual baris terpilih jadi input kuning (ceil 100), tombol "Simpan Harga (N)" muncul.
5. Simpan Harga → confirm modal (lama→baru) → Konfirmasi → toast sukses, tabel refresh.
6. Cek DB: `docker exec postgres-satset psql -U admin -d satset_go -c "SELECT code, price FROM product_denoms WHERE price IS NOT NULL ORDER BY code LIMIT 10;"`.
7. Regresi Task 2: buka `/admin/catalog/products/{id}/denoms` (per-produk) → inline edit + confirm + banner masih jalan identik.
8. User tanpa `manage_denoms`: markup bar + checkbox + input harga tidak muncul (kolom harga jadi teks).

- [ ] **Step 7: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/all-denoms.html
git commit -m "feat(catalog): all-denoms page with markup pricing bar

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```
