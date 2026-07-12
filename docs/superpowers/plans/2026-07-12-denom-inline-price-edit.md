# Denom Inline Price Edit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Inline edit harga jual (`price`) di tabel admin denoms, bulk submit satu request, confirmation modal (diff sebelum submit + hasil per-denom sesudah).

**Architecture:** Endpoint bulk baru `PUT /api/admin/catalog/denoms/prices` → `DenomDomainService.updatePrices()` dengan per-item result (partial success by design). UI: Alpine.js dirty-map di `denoms.html`, price cell jadi `<input>`, confirm modal reuse pola modal existing.

**Tech Stack:** Spring Boot 4, Java 25, JUnit 5 + AssertJ + Mockito (standalone MockMvc), Thymeleaf + Alpine.js + Tailwind/DaisyUI.

**Spec:** `docs/superpowers/specs/2026-07-12-denom-inline-price-edit-design.md`

## Global Constraints

- SKU (`code`) read-only — DF-owned, JANGAN pernah bisa diedit dari tabel.
- JANGAN expose `e.getMessage()` ke client — pesan error generic, detail hanya `log.error()`.
- TDD strict: test merah dulu, baru implementasi.
- Role guard endpoint denom: `OmniConstants.PERM_MANAGE_DENOMS` (`REALM_manage_denoms`).
- Semua test jalan dari worktree root: `/Users/kokohbudi/myProjects/satset-go/.claude/worktrees/live-edit-product-denom`.

---

### Task 1: Service `updatePrices` + DTO records

**Files:**
- Create: `satset-core/src/main/java/com/satset/catalog/dto/BulkPriceUpdateRequest.java`
- Create: `satset-core/src/main/java/com/satset/catalog/dto/PriceUpdateResult.java`
- Modify: `satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java` (tambah method di bawah `softDelete`, sekitar line 156)
- Test: `satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java`

**Interfaces:**
- Consumes: `DenomRepository.findById(UUID)`, `DenomRepository.save(ProductDenoms)` (existing).
- Produces: `List<PriceUpdateResult> updatePrices(List<BulkPriceUpdateRequest> items)` di `DenomDomainService`; records `BulkPriceUpdateRequest(UUID id, BigDecimal price)` dan `PriceUpdateResult(UUID id, String code, boolean ok, String error)` dengan factory `PriceUpdateResult.ok(id, code)` / `PriceUpdateResult.fail(id, code, error)`. Task 2 memakai persis ini.

- [ ] **Step 1: Tulis DTO records** (tanpa ini test tidak compile)

`satset-core/src/main/java/com/satset/catalog/dto/BulkPriceUpdateRequest.java`:

```java
package com.satset.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Satu item bulk update harga jual. Validasi price di service (per-item result, bukan 400 batch). */
public record BulkPriceUpdateRequest(UUID id, BigDecimal price) {}
```

`satset-core/src/main/java/com/satset/catalog/dto/PriceUpdateResult.java`:

```java
package com.satset.catalog.dto;

import java.util.UUID;

/** Hasil per-item bulk update harga. {@code error} null kalau ok. */
public record PriceUpdateResult(UUID id, String code, boolean ok, String error) {

    public static PriceUpdateResult ok(UUID id, String code) {
        return new PriceUpdateResult(id, code, true, null);
    }

    public static PriceUpdateResult fail(UUID id, String code, String error) {
        return new PriceUpdateResult(id, code, false, error);
    }
}
```

- [ ] **Step 2: Tulis failing tests**

Tambah di `DenomDomainServiceTest.java` (setelah test `softDelete`/update terakhir, dalam class). Tambah import:

```java
import com.satset.catalog.dto.BulkPriceUpdateRequest;
import com.satset.catalog.dto.PriceUpdateResult;
```

Test methods:

```java
    // === BULK PRICE UPDATE ===

    @Test
    void updatePrices_AllValid_UpdatesAndReturnsOk() {
        UUID otherId = UUID.randomUUID();
        ProductDenoms other = new ProductDenoms();
        other.setId(otherId);
        other.setCode("TLKM10");
        other.setPrice(new BigDecimal("10500.00"));
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.findById(otherId)).thenReturn(Optional.of(other));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PriceUpdateResult> results = denomService.updatePrices(List.of(
                new BulkPriceUpdateRequest(denomId, new BigDecimal("6000")),
                new BulkPriceUpdateRequest(otherId, new BigDecimal("11000"))));

        assertThat(results).hasSize(2).allMatch(PriceUpdateResult::ok);
        assertThat(existingDenom.getPrice()).isEqualByComparingTo("6000");
        assertThat(other.getPrice()).isEqualByComparingTo("11000");
        verify(denomRepository, times(2)).save(any(ProductDenoms.class));
    }

    @Test
    void updatePrices_NotFound_ItemError_OthersSucceed() {
        UUID missingId = UUID.randomUUID();
        when(denomRepository.findById(missingId)).thenReturn(Optional.empty());
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));
        when(denomRepository.save(any(ProductDenoms.class))).thenAnswer(inv -> inv.getArgument(0));

        List<PriceUpdateResult> results = denomService.updatePrices(List.of(
                new BulkPriceUpdateRequest(missingId, new BigDecimal("5000")),
                new BulkPriceUpdateRequest(denomId, new BigDecimal("6000"))));

        assertThat(results.get(0).ok()).isFalse();
        assertThat(results.get(0).error()).isEqualTo("Denom tidak ditemukan");
        assertThat(results.get(1).ok()).isTrue();
        verify(denomRepository, times(1)).save(any(ProductDenoms.class));
    }

    @Test
    void updatePrices_NonPositivePrice_ItemError_NoSave() {
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));

        List<PriceUpdateResult> results = denomService.updatePrices(List.of(
                new BulkPriceUpdateRequest(denomId, new BigDecimal("-1")),
                new BulkPriceUpdateRequest(denomId, BigDecimal.ZERO),
                new BulkPriceUpdateRequest(denomId, null)));

        assertThat(results).hasSize(3).noneMatch(PriceUpdateResult::ok);
        assertThat(results).allMatch(r -> "Harga harus > 0".equals(r.error()));
        verify(denomRepository, never()).save(any(ProductDenoms.class));
    }

    @Test
    void updatePrices_DeletedDenom_ItemError_NoSave() {
        existingDenom.setDeleted(true);
        when(denomRepository.findById(denomId)).thenReturn(Optional.of(existingDenom));

        List<PriceUpdateResult> results = denomService.updatePrices(List.of(
                new BulkPriceUpdateRequest(denomId, new BigDecimal("6000"))));

        assertThat(results.get(0).ok()).isFalse();
        assertThat(results.get(0).error()).isEqualTo("Denom sudah dihapus");
        verify(denomRepository, never()).save(any(ProductDenoms.class));
    }

    @Test
    void updatePrices_IsWriteTransactional() throws Exception {
        // Regression guard: class-level @Transactional(readOnly=true) — tanpa override
        // method-level, write tidak ke-flush. WAJIB @Transactional read-write.
        var tx = DenomDomainService.class
                .getMethod("updatePrices", List.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
    }
```

- [ ] **Step 3: Run test, verify FAIL**

```bash
cd /Users/kokohbudi/myProjects/satset-go/.claude/worktrees/live-edit-product-denom
mvn -q -pl satset-core test -Dtest=DenomDomainServiceTest 2>&1 | tail -20
```

Expected: COMPILATION ERROR — `method updatePrices ... cannot find symbol` (records ada, method belum).

- [ ] **Step 4: Implementasi minimal di `DenomDomainService`**

Tambah imports:

```java
import com.satset.catalog.dto.BulkPriceUpdateRequest;
import com.satset.catalog.dto.PriceUpdateResult;
import java.util.ArrayList;
```

Tambah methods setelah `softDelete` (sebelum section `// === Supplier sync (Digiflazz) ===`):

```java
    // === Bulk price update (inline edit harga jual) ===

    /**
     * Update harga jual banyak denom sekaligus. Error validasi (not found, harga ≤ 0,
     * deleted) → per-item result, dicek sebelum save. Persistensi satu transaksi:
     * ponytail: all-or-nothing — konflik optimistic-lock (edit bersamaan, langka)
     * menggagalkan seluruh batch, UI retain dirty state → user retry.
     */
    @Transactional
    public List<PriceUpdateResult> updatePrices(List<BulkPriceUpdateRequest> items) {
        List<PriceUpdateResult> results = new ArrayList<>(items.size());
        for (BulkPriceUpdateRequest item : items) {
            results.add(updateSinglePrice(item));
        }
        return results;
    }

    private PriceUpdateResult updateSinglePrice(BulkPriceUpdateRequest item) {
        Optional<ProductDenoms> found = denomRepository.findById(item.id());
        if (found.isEmpty()) {
            return PriceUpdateResult.fail(item.id(), null, "Denom tidak ditemukan");
        }
        ProductDenoms denom = found.get();
        if (item.price() == null || item.price().signum() <= 0) {
            return PriceUpdateResult.fail(item.id(), denom.getCode(), "Harga harus > 0");
        }
        if (denom.isDeleted()) {
            return PriceUpdateResult.fail(item.id(), denom.getCode(), "Denom sudah dihapus");
        }
        denom.setPrice(item.price());
        denomRepository.save(denom);
        return PriceUpdateResult.ok(item.id(), denom.getCode());
    }
```

PENTING: `@Transactional` method-level WAJIB — class-level `@Transactional(readOnly = true)` bikin method tanpa anotasi jalan read-only (write tidak ke-flush). Konsisten dengan semua method write lain di class ini. Tanpa try-catch di save: konflik optimistic-lock propagate → seluruh batch rollback → UI retry. Logger tidak diperlukan lagi (tidak ada exception yang di-swallow).

- [ ] **Step 5: Run test, verify PASS**

```bash
mvn -q -pl satset-core test -Dtest=DenomDomainServiceTest 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`, semua test (lama + 5 baru) hijau.

- [ ] **Step 6: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/dto/BulkPriceUpdateRequest.java \
        satset-core/src/main/java/com/satset/catalog/dto/PriceUpdateResult.java \
        satset-core/src/main/java/com/satset/catalog/service/DenomDomainService.java \
        satset-core/src/test/java/com/satset/catalog/service/DenomDomainServiceTest.java
git commit -m "feat(catalog): bulk denom price update service, per-item result

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: Endpoint `PUT /api/admin/catalog/denoms/prices`

**Files:**
- Modify: `satset-core/src/main/java/com/satset/catalog/web/AdminCatalogController.java` (section `// ==================== Denoms ====================`, setelah `updateDenom` line ~171)
- Test: `satset-core/src/test/java/com/satset/catalog/web/AdminCatalogControllerTest.java`

**Interfaces:**
- Consumes: `DenomDomainService.updatePrices(List<BulkPriceUpdateRequest>)` → `List<PriceUpdateResult>` (Task 1).
- Produces: `PUT /api/admin/catalog/denoms/prices`, body `[{"id":"<uuid>","price":1500}]`, response 200 `[{"id":"...","code":"byu10","ok":true,"error":null}, ...]`. Task 3 fetch endpoint ini.

- [ ] **Step 1: Tulis failing tests**

Tambah di `AdminCatalogControllerTest.java` (section Denom). Tambah imports:

```java
import com.satset.catalog.dto.PriceUpdateResult;
```

Test methods:

```java
    @Test
    void updateDenomPrices_ReturnsPerItemResults() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(manageDenomsUseCase.updatePrices(any())).thenReturn(List.of(
                PriceUpdateResult.ok(id1, "byu10"),
                PriceUpdateResult.fail(id2, "flash1", "Harga harus > 0")));

        String body = "[{\"id\":\"" + id1 + "\",\"price\":1500},{\"id\":\"" + id2 + "\",\"price\":-1}]";

        mockMvc.perform(put("/api/admin/catalog/denoms/prices")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ok").value(true))
                .andExpect(jsonPath("$[0].code").value("byu10"))
                .andExpect(jsonPath("$[1].ok").value(false))
                .andExpect(jsonPath("$[1].error").value("Harga harus > 0"));
    }

    @Test
    void updateDenomPrices_LiteralRouteWins_NotSingleDenomUpdate() throws Exception {
        // Guard: PUT /denoms/prices TIDAK boleh nyangkut ke PUT /denoms/{id} (UUID parse 400)
        when(manageDenomsUseCase.updatePrices(any())).thenReturn(List.of());

        mockMvc.perform(put("/api/admin/catalog/denoms/prices")
                        .contentType("application/json").content("[]"))
                .andExpect(status().isOk());

        verify(manageDenomsUseCase).updatePrices(any());
    }
```

- [ ] **Step 2: Run test, verify FAIL**

```bash
mvn -q -pl satset-core test -Dtest=AdminCatalogControllerTest 2>&1 | tail -20
```

Expected: FAIL — 2 test baru merah (404, endpoint belum ada). Test lama tetap hijau.

- [ ] **Step 3: Implementasi endpoint**

Di `AdminCatalogController.java`, tambah imports:

```java
import com.satset.catalog.dto.BulkPriceUpdateRequest;
import com.satset.catalog.dto.PriceUpdateResult;
```

Tambah method setelah `updateDenom` (sebelum `deleteDenom`):

```java
    @PutMapping("/denoms/prices")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<List<PriceUpdateResult>> updateDenomPrices(
            @RequestBody List<BulkPriceUpdateRequest> req) {
        return ResponseEntity.ok(manageDenomsUseCase.updatePrices(req));
    }
```

Catatan: validasi price di service (per-item error), bukan bean validation — satu item invalid tidak boleh 400-kan seluruh batch. Route literal `/denoms/prices` menang atas `/denoms/{id}` (PathPattern specificity).

- [ ] **Step 4: Run test, verify PASS**

```bash
mvn -q -pl satset-core test -Dtest=AdminCatalogControllerTest 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`, semua hijau.

- [ ] **Step 5: Commit**

```bash
git add satset-core/src/main/java/com/satset/catalog/web/AdminCatalogController.java \
        satset-core/src/test/java/com/satset/catalog/web/AdminCatalogControllerTest.java
git commit -m "feat(catalog): PUT /denoms/prices bulk price endpoint

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: UI inline edit + confirm modal di `denoms.html`

**Files:**
- Modify: `satset-core/src/main/resources/templates/pages/admin/catalog/denoms.html`

**Interfaces:**
- Consumes: `PUT /api/admin/catalog/denoms/prices` (Task 2); Alpine stores existing `Alpine.store('toast')`; state existing `canManage`, `denoms`, `loadDenoms()`, `formatRp()`.
- Produces: UI only, tidak ada consumer lanjutan.

- [ ] **Step 1: Price cell → inline input**

Ganti line 87 (`<td class="text-right font-mono" x-text="formatRp(d.price)"></td>`) dengan:

```html
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
```

- [ ] **Step 2: Tombol "Simpan Harga (N)" di header**

Di dalam `<div class="flex gap-2">` (line 35), sebelum tombol "Sync Denom DF", tambah:

```html
            <button x-show="dirtyList.length > 0" x-cloak class="btn btn-warning tap"
                    @click="openPriceModal()">
                Simpan Harga (<span x-text="dirtyList.length"></span>)
            </button>
```

(Tanpa `sec:authorize` — tombol hanya muncul kalau ada dirty, dan dirty hanya bisa terjadi dari input yang di-gate `canManage`.)

- [ ] **Step 3: Confirmation modal**

Tambah setelah Sync Preview Modal (setelah closing div-nya, sekitar line 276), sebelum `</div>` penutup `layout:fragment`:

```html
    <!-- Bulk Price Confirm Modal -->
    <div class="modal" :class="{ 'modal-open': showPriceModal }">
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
```

- [ ] **Step 4: Alpine state + methods**

Di `denomManager()` return object, tambah state setelah `compareBySku: {},`:

```js
            dirty: {},
            showPriceModal: false,
            savingPrices: false,
            priceResults: [],
```

Tambah methods setelah `loadDenoms()` (sebelum `loadCompare()`):

```js
            // === Inline price edit ===
            get dirtyList() { return Object.values(this.dirty); },
            isDirty(d) { return !!this.dirty[d.id]; },
            pendingPrice(d) { return this.dirty[d.id] ? this.dirty[d.id].newPrice : d.price; },
            resultFor(id) { return this.priceResults.find(r => r.id === id); },

            setPrice(d, val) {
                const p = val === '' ? null : Number(val);
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
                        if (r.ok) delete this.dirty[r.id];   // yang gagal tetap dirty, bisa dicoba lagi
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
            },
```

Catatan perilaku: setelah submit dengan kegagalan, modal TETAP terbuka menampilkan badge hasil per-row (`dirtyList` masih berisi yang gagal, `priceResults` kasih badge OK/Gagal untuk semua yang barusan disubmit — row sukses hilang dari list karena dirty-nya dihapus; badge hanya terlihat untuk row gagal, itu yang penting).

- [ ] **Step 5: Verifikasi build + full test**

```bash
mvn -q -pl satset-core test 2>&1 | tail -15
```

Expected: `BUILD SUCCESS` (template tidak dicompile test, ini regression check backend).

- [ ] **Step 6: Verifikasi manual di browser**

Jalankan app (profil dev, DB + Keycloak lokal harus hidup):

```bash
mvn -pl satset-core spring-boot:run -Dspring-boot.run.profiles=dev
```

Cek sebagai admin dengan role `manage_denoms` di halaman denoms sebuah product:
1. Ubah 2 harga di kolom "Harga Jual" → input jadi kuning (warning), tombol "Simpan Harga (2)" muncul.
2. Balikin satu ke nilai awal → counter jadi (1).
3. Klik tombol → modal diff tampil (code, nama, lama → baru).
4. Konfirmasi → toast sukses, tabel refresh, harga baru tampil, tombol hilang.
5. Cek DB: `docker exec postgres-satset psql -U admin -d satset_go -c "SELECT code, price FROM product_denoms WHERE price IS NOT NULL LIMIT 5;"`
6. User tanpa role `manage_denoms`: kolom harga tetap teks biasa (bukan input).

- [ ] **Step 7: Commit**

```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/denoms.html
git commit -m "feat(catalog): inline price edit + bulk confirm modal on denoms page

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```
