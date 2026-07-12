# Catalog Integration: Fold Bulk-Pricing into Denom-Centric Page — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Merge main's "Semua Denom" bulk-pricing/editing (markup, inline Nama/Harga Jual edit, arrow-nav, unpriced banner) into the denom-centric `index.html`, then retire the standalone `all-denoms.html` page and its now-redundant backend.

**Architecture:** Reuse the existing shared price-editing mixin (`static/js/denom-price-editing.js`), the price-confirm fragment, and the `PUT /denoms/prices` / `PUT /denoms/names` endpoints. Port the name-editing, markup, arrow-nav, and checkbox-selection logic from `all-denoms.html` into `index.html`'s Alpine `catalogManager()`. Remove `all-denoms.html`, its page route, `GET /denoms/list`, `DenomListItemDTO`, and `findAllForList`.

**Tech Stack:** Spring Boot 4, Java 25, Thymeleaf + Alpine.js + Tailwind/daisyUI, JUnit 5 + Mockito + MockMvc.

## Global Constraints

- Base branch is at main `61c480e`; `index.html` = the denom-centric page (Harga Suplier, Sinkronkan, Samakan, combobox create, DF error). Do not regress those.
- No schema change; buyer/purchase/transaction untouched. No new denom DTO fields — client resolves category/product names via `catNameOf`/`prodNameOf`.
- Reuse `PUT /api/admin/catalog/denoms/prices` and `/denoms/names` **unchanged**.
- Two selection models coexist by design: **checkbox** targets markup (Harga Jual); **Samakan** stays scope-aware (all differing rows in the active filter). Do not unify them.
- Errors via `Alpine.store('toast')`, not raw messages. BI copy. `prefers-reduced-motion` respected on any new animation; touch targets ≥44px.
- Build: `mvn` (no wrapper). Single test class: `mvn -q -pl satset-core test -Dtest=<Class>`. Frontend has no JS unit harness → compile + manual browser verify.
- Commit after each task.

## File Structure

- `satset-core/.../web/AdminCatalogPageController.java` (modify) — drop the `/denoms`→all-denoms route.
- `satset-core/.../web/AdminCatalogController.java` (modify) — drop `GET /denoms/list`.
- `satset-core/.../service/DenomDomainService.java` (modify) — drop `findAllForList()`.
- `satset-core/.../dto/DenomListItemDTO.java` (delete).
- `satset-core/.../templates/pages/admin/catalog/all-denoms.html` (delete).
- `satset-core/.../templates/pages/admin/catalog/index.html` (modify) — the integration.
- Keep: `static/js/denom-price-editing.js`, `templates/fragments/denom-price-confirm-modal.html`.
- Tests: `AdminCatalogControllerTest`, `AdminCatalogPageControllerTest`, `DenomDomainServiceTest` (modify).

---

## Phase A — Backend retirement

### Task 1: Retire all-denoms page + /denoms/list + DenomListItemDTO + findAllForList

**Files:**
- Modify: `AdminCatalogPageController.java`, `AdminCatalogController.java`, `DenomDomainService.java`
- Delete: `dto/DenomListItemDTO.java`, `templates/pages/admin/catalog/all-denoms.html`
- Test: `AdminCatalogControllerTest.java`, `AdminCatalogPageControllerTest.java`, `DenomDomainServiceTest.java`

**Interfaces:**
- Produces: none (pure removal). `GET /denoms` (ProductDenomDTO), `PUT /denoms/prices`, `PUT /denoms/names` remain.

- [ ] **Step 1: Remove the tests for the retired pieces first**

In `AdminCatalogControllerTest.java` delete the `listDenomsForList_ReturnsEnrichedDTOs` test method (the one performing `get("/api/admin/catalog/denoms/list")`). In `DenomDomainServiceTest.java` delete any test that calls `findAllForList()`. In `AdminCatalogPageControllerTest.java` delete any assertion/test for the `all-denoms` view/route (keep the `index` one). Remove now-unused imports (`DenomListItemDTO`).

- [ ] **Step 2: Remove the route + endpoint + service method + DTO + template**

`AdminCatalogPageController.java` — delete the method mapping `@GetMapping("/denoms")` that returns `"pages/admin/catalog/all-denoms"` (keep `@GetMapping({"", "/categories"})` → `index`).

`AdminCatalogController.java` — delete:
```java
@GetMapping("/denoms/list")
@PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
public ResponseEntity<List<DenomListItemDTO>> listDenomsForList() {
    return ResponseEntity.ok(manageDenomsUseCase.findAllForList());
}
```
and its `import com.satset.catalog.dto.DenomListItemDTO;` if unused after.

`DenomDomainService.java` — delete `findAllForList()` and its `DenomListItemDTO` import if unused.

Delete files:
```bash
git rm satset-core/src/main/java/com/satset/catalog/dto/DenomListItemDTO.java
git rm satset-core/src/main/resources/templates/pages/admin/catalog/all-denoms.html
```

- [ ] **Step 3: Verify no dangling references**

Run:
```bash
grep -rn "DenomListItemDTO\|findAllForList\|/denoms/list\|all-denoms" satset-core/src
```
Expected: no matches (or only in docs/plans). If a `catalog-code`/nav references `all-denoms` route, remove it.

- [ ] **Step 4: Compile + run affected tests**

Run: `mvn -q -pl satset-core test -Dtest=AdminCatalogControllerTest,AdminCatalogPageControllerTest,DenomDomainServiceTest`
Expected: PASS, no compile errors.

- [ ] **Step 5: Commit**

```bash
git add -A satset-core/src/main/java/com/satset/catalog satset-core/src/test/java/com/satset/catalog
git commit -m "refactor(catalog): retire all-denoms page + /denoms/list; index.html will absorb it"
```

---

## Phase B — Frontend integration (`index.html`)

All Phase-B tasks modify only `satset-core/src/main/resources/templates/pages/admin/catalog/index.html`. No JS unit harness → each task: keep it compiling/rendering, self-review the script for balance + no dangling refs, commit. Controller runs a browser smoke after Task 6. Reference source: `all-denoms.html` (still in git history after Task 1 — use `git show HEAD~1:satset-core/.../all-denoms.html` if needed) or copy from the line numbers cited (captured pre-deletion below).

### Task 2: Mix in price-editing + port name-editing state/methods

**Files:** Modify `index.html` (scripts block + `catalogManager()` wrapper).

**Interfaces:**
- Consumes: `static/js/denom-price-editing.js` (`window.denomPriceEditing(getDenoms)`), `refreshDenoms()`, `formatRp`.
- Produces (later tasks use): mixin members (`dirty`,`dirtyList`,`isDirty`,`pendingPrice`,`setPrice`,`unpricedCount`,`filterUnpriced`,`openPriceModal`,`submitPrices`,`showPriceModal`,`savingPrices`,`priceResults`,`resultFor`); name-editing members (`nameDirty`,`nameDirtyList`,`isNameDirty`,`pendingName`,`nameResultFor`,`setName`,`openNameModal`,`submitNames`,`showNameModal`,`savingNames`,`nameResults`).

- [ ] **Step 1: Load the shared mixin script**

In the `<th:block layout:fragment="scripts">` (index.html:518), immediately before the existing `<script th:inline="javascript">`, add:
```html
<!-- no defer: define window.denomPriceEditing before Alpine auto-starts -->
<script th:src="@{/js/denom-price-editing.js}"></script>
```

- [ ] **Step 2: Wrap the component to mix in the price editor**

Change `catalogManager()` open (index.html:526-527) from `return {` to `const c = {`, and its close (index.html:999-1000) from:
```javascript
        };
    }
```
to:
```javascript
            loadDenoms() { return this.refreshDenoms(); },   // alias for the shared mixin
        };
        // mix in shared price-editing WITHOUT invoking getters (spread would eager-eval get unpricedCount)
        Object.defineProperties(c, Object.getOwnPropertyDescriptors(window.denomPriceEditing(() => c.denoms)));
        return c;
    }
```
(Place the `loadDenoms` alias as the last property inside the object literal, before the closing `};`.)

- [ ] **Step 3: Add name-editing state + methods**

Inside the object literal (near the existing supplier/apply methods), add state `nameDirty: {}, showNameModal: false, savingNames: false, nameResults: [],` and these methods (ported from `all-denoms.html` lines 264–299, `loadDenoms()`→`refreshDenoms()`):
```javascript
get nameDirtyList() { return Object.values(this.nameDirty); },
isNameDirty(d) { return !!this.nameDirty[d.id]; },
pendingName(d) { return this.nameDirty[d.id] ? this.nameDirty[d.id].newName : d.name; },
nameResultFor(id) { return this.nameResults.find(r => r.id === id); },
setName(d, val) {
    const name = (val ?? '').trim();
    if (name === '' || name === d.name) delete this.nameDirty[d.id];
    else this.nameDirty[d.id] = { id: d.id, code: d.code, oldName: d.name, newName: name };
},
openNameModal() { this.nameResults = []; this.showNameModal = true; },
async submitNames() {
    this.savingNames = true;
    try {
        const res = await fetch('/api/admin/catalog/denoms/names', {
            method: 'PUT', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(this.nameDirtyList.map(x => ({ id: x.id, name: x.newName })))
        });
        if (!res.ok) throw new Error('Gagal menyimpan nama');
        this.nameResults = await res.json();
        const okCount = this.nameResults.filter(r => r.ok).length;
        const failCount = this.nameResults.length - okCount;
        for (const r of this.nameResults) { if (r.ok) delete this.nameDirty[r.id]; }
        await this.refreshDenoms();
        if (failCount === 0) { this.showNameModal = false; Alpine.store('toast').success(`${okCount} nama diperbarui`); }
        else Alpine.store('toast').error(`${failCount} gagal disimpan, ${okCount} sukses`);
    } catch (e) { Alpine.store('toast').error(e.message); }
    finally { this.savingNames = false; }
},
```

- [ ] **Step 4: Verify + commit**

`mvn -q -pl satset-core compile` (template unaffected but confirms build). Re-read the wrapped script: braces balanced, `const c` / `return c` correct, mixin line present. Load the page mentally — component still constructs (mixin members added, none referenced by markup yet, so no behavior change). Commit:
```bash
git add satset-core/src/main/resources/templates/pages/admin/catalog/index.html
git commit -m "feat(catalog-ui): mix in price-editing + name-editing into catalogManager"
```

---

### Task 3: Checkbox select column + markup bar

**Files:** Modify `index.html` (state, methods, toolbar markup, table checkbox column).

**Interfaces:**
- Consumes: `filteredDenoms` (existing getter, index.html:678), mixin `setPrice`.
- Produces: `selectedIds`, `showMarkup`, `markup`, `allVisibleSelected()`, `toggleSelectAll()`, `applyMarkup()`.

- [ ] **Step 1: State + methods**

Add state `selectedIds: [], showMarkup: false, markup: { base: 'MODAL', type: 'PERSEN', value: null },` and methods (ported from `all-denoms.html` 232–262):
```javascript
allVisibleSelected() {
    const vis = this.filteredDenoms.filter(d => !d.deleted);
    return vis.length > 0 && vis.every(d => this.selectedIds.includes(d.id));
},
toggleSelectAll(checked) {
    const visIds = this.filteredDenoms.filter(d => !d.deleted).map(d => d.id);
    this.selectedIds = checked ? [...new Set([...this.selectedIds, ...visIds])]
                               : this.selectedIds.filter(id => !visIds.includes(id));
},
applyMarkup() {
    if (this.selectedIds.length === 0) { Alpine.store('toast').error('Centang dulu denom yang mau diatur'); return; }
    const v = Number(this.markup.value);
    if (this.markup.value === null || this.markup.value === '' || !(v >= 0)) { Alpine.store('toast').error('Isi dulu besar kenaikannya'); return; }
    let skipped = 0, applied = 0;
    for (const id of this.selectedIds) {
        const d = this.denoms.find(x => x.id === id);
        if (!d || d.deleted) continue;
        const base = this.markup.base === 'MODAL' ? d.basePrice : (d.price ?? d.basePrice);
        if (base == null) { skipped++; continue; }
        const raw = this.markup.type === 'PERSEN' ? Number(base) * (1 + v / 100) : Number(base) + v;
        this.setPrice(d, Math.ceil(raw / 100) * 100);
        applied++;
    }
    if (skipped > 0) Alpine.store('toast').error(`${applied} di-set, ${skipped} dilewati (tak ada modal/harga)`);
    else Alpine.store('toast').success(`${applied} harga di-set, cek & simpan`);
},
```

- [ ] **Step 2: Markup bar markup**

After the page header row (below the `</div>` at index.html:27, before the tab list), add:
```html
<div sec:authorize="hasRole('REALM_manage_denoms')" x-show="showMarkup" x-cloak class="card bg-base-100 shadow-sm p-4 mb-4">
    <div class="flex flex-wrap items-end gap-3">
        <label class="form-control"><span class="label-text text-xs">Hitung dari</span>
            <select class="select select-bordered select-sm" x-model="markup.base">
                <option value="MODAL">Harga Beli</option><option value="CURRENT">Harga Jual Sekarang</option>
            </select></label>
        <label class="form-control"><span class="label-text text-xs">Naikkan pakai</span>
            <select class="select select-bordered select-sm" x-model="markup.type">
                <option value="PERSEN">Persen (%)</option><option value="FIX">Nominal (Rp)</option>
            </select></label>
        <label class="form-control"><span class="label-text text-xs">Besarnya</span>
            <input type="number" min="0" step="any" class="input input-bordered input-sm w-32" x-model.number="markup.value"/></label>
        <button class="btn btn-primary btn-sm tap" @click="applyMarkup()">Terapkan ke terpilih (<span x-text="selectedIds.length"></span>)</button>
    </div>
</div>
```

- [ ] **Step 3: Checkbox column in the denom table**

In the denom `<thead>` row (index.html:111+), add as the FIRST `<th>`:
```html
<th sec:authorize="hasRole('REALM_manage_denoms')">
    <input type="checkbox" class="checkbox checkbox-sm" @change="toggleSelectAll($event.target.checked)" :checked="allVisibleSelected()"/>
</th>
```
In the row template (`x-for="d in filteredDenoms"`, index.html:133 — change to `x-for="(d, idx) in filteredDenoms"` for Task 4), add as the FIRST `<td>`:
```html
<td sec:authorize="hasRole('REALM_manage_denoms')">
    <input type="checkbox" class="checkbox checkbox-sm" :value="d.id" x-model="selectedIds" :disabled="d.deleted"/>
</td>
```
Bump the empty-row `colspan` (index.html:179) from 12 to 13.

- [ ] **Step 4: Toolbar "Atur Harga Massal" toggle**

In the header button group (near index.html:23-30, the Samakan/Sinkronkan buttons), add:
```html
<button sec:authorize="hasRole('REALM_manage_denoms')" x-show="canManageDenom" class="btn btn-outline btn-sm tap" @click="showMarkup = !showMarkup">Atur Harga Massal</button>
```

- [ ] **Step 5: Verify + commit**

`mvn -q -pl satset-core compile`. Manual (after restart): checkbox select-all + per-row work; "Atur Harga Massal" toggles the bar; markup on checked rows sets Harga Jual to pending (dirty shows once Task 4 renders inputs — for now verify `denoms` pending via console `$data.dirtyList`). Commit:
```bash
git commit -am "feat(catalog-ui): checkbox select + markup massal (Harga Jual)"
```

---

### Task 4: Inline Nama + Harga Jual cells + arrow-nav

**Files:** Modify `index.html` (row cells for Nama + Harga Jual, arrow-nav methods).

**Interfaces:**
- Consumes: mixin `isDirty`,`pendingPrice`,`setPrice`; name `isNameDirty`,`pendingName`,`setName`; `(d, idx)` row index.
- Produces: `cellNav(e)`, `focusCell(r,c,dir)`.

- [ ] **Step 1: Arrow-nav methods**

Add (verbatim from `all-denoms.html` 313–345):
```javascript
cellNav(e) {
    const key = e.key;
    if (!key.startsWith('Arrow')) return;
    const el = e.target; const r = Number(el.dataset.r); const c = el.dataset.c;
    const isText = el.type !== 'number';
    if (key === 'ArrowUp' || key === 'ArrowDown') {
        e.preventDefault(); const dir = key === 'ArrowDown' ? 1 : -1; this.focusCell(r + dir, c, dir);
    } else if (key === 'ArrowLeft' && c === 'price') {
        e.preventDefault(); this.focusCell(r, 'name', 0);
    } else if (key === 'ArrowRight' && c === 'name') {
        const atEnd = !isText || (el.selectionStart === el.value.length && el.selectionEnd === el.value.length);
        if (atEnd) { e.preventDefault(); this.focusCell(r, 'price', 0); }
    }
},
focusCell(r, c, dir) {
    const max = this.filteredDenoms.length; let rr = r;
    while (rr >= 0 && rr < max) {
        const t = document.querySelector(`input[data-r="${rr}"][data-c="${c}"]`);
        if (t) { t.focus(); return; }
        if (dir === 0) return; rr += dir;
    }
},
```

- [ ] **Step 2: Nama cell → inline editable**

Replace the existing Nama `<td>` (currently `<td x-text="d.name"></td>` in the row) with:
```html
<td>
    <template x-if="canManageDenom && !d.deleted">
        <input type="text" maxlength="150" class="input input-bordered input-xs w-48"
               :class="isNameDirty(d) ? 'input-warning' : ''" :value="pendingName(d)"
               :data-r="idx" data-c="name" @keydown="cellNav($event)" @change="setName(d, $event.target.value)"/>
    </template>
    <template x-if="!canManageDenom || d.deleted"><span x-text="d.name"></span></template>
</td>
```

- [ ] **Step 3: Harga Jual cell → inline editable**

Replace the Harga Jual `<td>` (currently `<td class="text-right font-mono" x-text="formatRp(d.price)"></td>`) with:
```html
<td class="text-right">
    <template x-if="canManageDenom && !d.deleted">
        <input type="number" min="1" step="any" class="input input-bordered input-xs w-28 text-right font-mono"
               :class="isDirty(d) ? 'input-warning' : ''" :value="pendingPrice(d)"
               :data-r="idx" data-c="price" @keydown="cellNav($event)" @change="setPrice(d, $event.target.value)"/>
    </template>
    <template x-if="!canManageDenom || d.deleted"><span class="font-mono" x-text="formatRp(d.price)"></span></template>
</td>
```

- [ ] **Step 4: Verify + commit**

`mvn -q -pl satset-core compile`. Manual (restart): edit a Nama and a Harga Jual cell → turns yellow (dirty); arrow keys move focus between Nama↔Harga Jual and up/down rows, skipping deleted; markup fills Harga Jual cells yellow. Commit:
```bash
git commit -am "feat(catalog-ui): inline edit Nama + Harga Jual with arrow-nav"
```

---

### Task 5: Save buttons, unpriced banner, confirm modals

**Files:** Modify `index.html` (toolbar save buttons, unpriced banner, `filteredDenoms` unpriced filter, price+name confirm modals).

**Interfaces:**
- Consumes: mixin `dirtyList`,`openPriceModal`,`filterUnpriced`,`unpricedCount`,`showPriceModal`; name `nameDirtyList`,`openNameModal`,`showNameModal`,`submitNames`,`nameResultFor`; fragment `denom-price-confirm-modal`.

- [ ] **Step 1: Save buttons in the toolbar**

In the header button group, add (before/after Atur Harga Massal):
```html
<button sec:authorize="hasRole('REALM_manage_denoms')" x-show="nameDirtyList.length > 0" x-cloak class="btn btn-warning btn-sm tap" @click="openNameModal()">Simpan Nama (<span x-text="nameDirtyList.length"></span>)</button>
<button sec:authorize="hasRole('REALM_manage_denoms')" x-show="dirtyList.length > 0" x-cloak class="btn btn-warning btn-sm tap" @click="openPriceModal()">Simpan Harga (<span x-text="dirtyList.length"></span>)</button>
```

- [ ] **Step 2: Unpriced banner + filter**

After the markup bar (Task 3), add:
```html
<div x-show="unpricedCount > 0" x-cloak class="alert alert-warning py-2 mb-4 flex items-center justify-between">
    <span>&#9888; <b x-text="unpricedCount"></b> denom belum ada harga jual</span>
    <button class="btn btn-xs tap" @click="filterUnpriced = !filterUnpriced" x-text="filterUnpriced ? 'Lihat semua' : 'Lihat yang belum ada harga'"></button>
</div>
```
Fold `filterUnpriced` into the `filteredDenoms` getter (index.html:678): at the start of building the list, add `if (this.filterUnpriced && this.unpricedCount === 0) this.filterUnpriced = false;` and after the scope filter, `if (this.filterUnpriced) list = list.filter(d => !d.deleted && d.price == null);` (apply before/with the search filter; keep existing scope + search logic intact).

- [ ] **Step 3: Price confirm modal (fragment) + Name confirm modal**

Before the closing `</div>` of the content fragment (near the other modals), add:
```html
<div th:replace="~{fragments/denom-price-confirm-modal :: modal}"></div>
```
and the Name confirm modal — copy `all-denoms.html` lines 152–192 verbatim (it references `nameDirtyList`,`nameResults`,`nameResultFor`,`submitNames`,`savingNames`,`showNameModal` — all present from Task 2).

- [ ] **Step 4: Verify + commit**

`mvn -q -pl satset-core compile`. Manual (restart): edit prices/names → "Simpan Harga (N)" / "Simpan Nama (N)" appear → clicking opens the confirm modal → Konfirmasi saves (PUT), table refetches, dirty clears; unpriced banner shows a count and the filter toggles. Commit:
```bash
git commit -am "feat(catalog-ui): bulk-save buttons, unpriced banner, price+name confirm modals"
```

---

## Phase C — Verification

### Task 6: Full test + browser smoke

- [ ] **Step 1: Run catalog + supplier tests**

Run: `mvn -pl satset-core test -Dtest='com.satset.catalog.**,com.satset.supplier.**'`
Expected: PASS, no regressions.

- [ ] **Step 2: Browser smoke (controller drives, or hand to user — needs Postgres+KC+auth on :8081)**

Restart the app, load `/admin/catalog`, verify end to end:
- Existing: filter tabs + auto-prune, Harga Suplier + DF error caption, Samakan (per-row + bulk), Sinkronkan, combobox create, Tambah/Edit denom.
- New: checkbox select-all/per-row; Atur Harga Massal → markup on checked rows fills Harga Jual yellow; inline edit Nama + Harga Jual; arrow-nav; Simpan Harga / Simpan Nama → confirm modal → saved; unpriced banner + filter.
- `all-denoms.html` gone: `/admin/catalog/denoms` no longer serves a page (route removed).

- [ ] **Step 3: Update tracking + finish**

Update `Tasks.md` + the Google Tasks list. Then use `superpowers:finishing-a-development-branch`.

---

## Self-Review notes

- **Spec coverage:** retire backend + template (T1); mixin + name-edit (T2); checkbox + markup (T3); inline cells + arrow-nav (T4); save buttons + banner + modals (T5); verify (T6). All spec sections mapped.
- **Type/name consistency:** mixin members (`setPrice`,`pendingPrice`,`isDirty`,`dirtyList`,`unpricedCount`,`filterUnpriced`,`openPriceModal`,`submitPrices`) and name members used identically across T2–T5; `data-r`/`data-c` match between cells (T4) and `cellNav`/`focusCell`.
- **Ceilings:** wide table (horizontal scroll); name-editing not yet extracted to a shared mixin (noted); two selection models intentional.
