# Catalog Integration: Fold Bulk-Pricing into the Denom-Centric Page — Design

**Date:** 2026-07-12
**Base:** main `61c480e` (has both `index.html` denom-centric page and `all-denoms.html` bulk-pricing page)
**Scope:** Merge main's "Semua Denom" bulk-pricing/editing features into the denom-centric `index.html`, then retire the separate `all-denoms.html` page. `catalog` slice + one supplier-independent template. No schema change.

## Goal

One catalog admin page (`/admin/catalog`, `index.html`) that does everything: filter tabs + Harga Suplier + sync (existing) **plus** bulk Harga Jual markup, inline edit of Nama & Harga Jual with spreadsheet-style arrow-nav, and the unpriced-denom banner (ported from `all-denoms.html`). The standalone `all-denoms.html` page is removed.

## Why

Main and the denom-centric branch built complementary tools on the same denom table: main = bulk Harga Jual (sell price) editing; branch = structure + Harga Beli (cost) sync. They were merged coexisting (two pages); `all-denoms.html`'s tab-nav now links to retired pages (`/categories`, `/products`) and is partially broken. Folding into one page fixes that and gives a single surface.

## Decisions (user-confirmed)

- **Markup targeting:** a new checkbox-select column; "Atur Harga Massal" applies to **checked** rows only.
- **Samakan (Harga Beli = Harga Suplier):** unchanged — per-row button + scope-aware bulk over all differing rows in the active filter. (Deliberately NOT checkbox-driven; two selection models coexist: checkbox for markup, scope for Samakan.)
- **Inline vs modal:** Nama & Harga Jual editable inline in the table (dirty-tracked, arrow-nav, bulk save); the existing full Edit modal stays for other fields (tipe, nominal, admin fee, combobox move category/product).

## Non-goals

- No schema change; no change to buyer catalog, purchase, transaction.
- No change to the DF/supplier features already in `index.html` (Harga Suplier, Sinkronkan, Samakan, combobox create, DF error surfacing).
- Not touching `PUT /denoms/prices` / `PUT /denoms/names` behavior — reused as-is.

---

## Backend — mostly retirement

Reuse (no change): `PUT /api/admin/catalog/denoms/prices` (`updatePrices`), `PUT /api/admin/catalog/denoms/names` (`updateNames`), their service methods, `PriceUpdateResult`, `BulkPriceUpdateRequest`, `BulkNameUpdateRequest`.

**Retire (index.html covers it via `GET /denoms` + ProductDenomDTO):**
- `AdminCatalogPageController`: remove the `@GetMapping("/denoms")` → `all-denoms` route + its model seeding (the `index` route stays).
- `AdminCatalogController`: remove `@GetMapping("/denoms/list")` (`listDenomsForList`).
- `DenomDomainService`: remove `findAllForList()`.
- Remove `DenomListItemDTO` (only consumer was the retired endpoint/page).
- `git rm` `templates/pages/admin/catalog/all-denoms.html`.
- Tests: remove `listDenomsForList_ReturnsEnrichedDTOs` from `AdminCatalogControllerTest`; remove any `findAllForList` test in `DenomDomainServiceTest`; remove the page-route assertion for `all-denoms` in `AdminCatalogPageControllerTest` if present.

Keep: `static/js/denom-price-editing.js` (shared mixin) and `templates/fragments/denom-price-confirm-modal.html` — now consumed by `index.html`.

---

## Frontend — `index.html` (Alpine `catalogManager()`)

### Reuse the price-editing mixin
Load the shared script (no defer, before Alpine) and mix it into the component the same way `all-denoms.html` did:

```html
<script th:src="@{/js/denom-price-editing.js}"></script>
```
```javascript
function catalogManager() {
    const c = { /* existing state + getters + methods */ };
    // mix in shared price editing (dirty Harga Jual, submit, unpriced) WITHOUT invoking getters
    Object.defineProperties(c, Object.getOwnPropertyDescriptors(window.denomPriceEditing(() => c.denoms)));
    return c;
}
```
The mixin requires host to provide `getDenoms()` (passed as `() => c.denoms`), `formatRp(v)` (exists), `loadDenoms()`, and Alpine `toast` store (exists). Add an alias so the mixin's `this.loadDenoms()` hits the existing refetch:

```javascript
loadDenoms() { return this.refreshDenoms(); },
```
Mixin provides: `dirty`, `dirtyList`, `isDirty(d)`, `pendingPrice(d)`, `setPrice(d,val)`, `unpricedCount`, `filterUnpriced`, `openPriceModal()`, `submitPrices()`, `showPriceModal`, `savingPrices`, `priceResults`, `resultFor(id)`.

### Port name-editing (inline, from all-denoms.html)
Add to the component: `nameDirty: {}`, `showNameModal`, `savingNames`, `nameResults`, and getters/methods `nameDirtyList`, `isNameDirty(d)`, `pendingName(d)`, `nameResultFor(id)`, `setName(d,val)`, `openNameModal()`, `submitNames()` (PUT `/denoms/names`, then `refreshDenoms()`). Copy verbatim from `all-denoms.html` lines 264–299, changing `loadDenoms()` → `refreshDenoms()`.

### Port markup bar + selection
- State: `selectedIds: []`, `showMarkup: false`, `markup: { base:'MODAL', type:'PERSEN', value:null }`.
- Methods: `allVisibleSelected()`, `toggleSelectAll(checked)`, `applyMarkup()` — copy from `all-denoms.html` lines 232–262, but `toggleSelectAll`/`allVisibleSelected` iterate the current `filteredDenoms` (index's scoped+searched getter). `applyMarkup()` rounds up to 100 and calls the mixin `setPrice`.
- Markup bar markup (Atur Harga Massal card) + "Terapkan ke terpilih (N)" — ported into the toolbar area.

### Port arrow-nav
Copy `cellNav(e)` + `focusCell(r,c,dir)` from `all-denoms.html` lines 313–345 verbatim (they key off `data-r`/`data-c` on the Nama/Harga Jual inputs).

### Table changes
- New leading **checkbox** column (`sec:authorize` manage_denoms): header select-all + per-row `x-model="selectedIds"`.
- **Nama** cell → inline `<input type="text">` when `canManageDenom && !d.deleted` (dirty → `input-warning`, `:value="pendingName(d)"`, `@change="setName(...)"`, `data-r`/`data-c="name"`, `@keydown="cellNav"`); else plain text.
- **Harga Jual** cell → inline `<input type="number">` similarly (`pendingPrice`, `setPrice`, `data-c="price"`).
- Keep **Harga Beli**, **Harga Suplier** (+flag/date), **Samakan** button, **Kategori/Produk**, **Aksi (Edit/Hapus)** as-is.
- Row index `(d, idx)` needed for `data-r`.
- Wide table: keep `overflow-x-auto`; row height stays compact (`table-sm`).

### Toolbar / header buttons
Add near the existing Sinkronkan/Samakan group: **Atur Harga Massal** (toggle), **Simpan Harga (N)** (`x-show="dirtyList.length"`, `@click="openPriceModal()"`), **Simpan Nama (N)** (`x-show="nameDirtyList.length"`, `@click="openNameModal()"`).

### Unpriced banner
Port the `unpricedCount > 0` warning + "Lihat yang belum ada harga" toggle (`filterUnpriced`); fold `filterUnpriced` into `filteredDenoms` (price == null).

### Modals
- `th:replace="~{fragments/denom-price-confirm-modal :: modal}"` for the price confirm.
- Port the Name confirm modal markup (all-denoms.html lines 152–192).

### Keep everything existing
Filter tabs + auto-prune, combobox denom modal (create cat/product), Sinkronkan, Samakan (per-row + bulk), Harga Suplier column + DF error, Tambah Denom.

---

## Testing

- `AdminCatalogControllerTest`: drop the `/denoms/list` test; keep `/denoms` (ProductDenomDTO) test + the prices/names bulk tests.
- `DenomDomainServiceTest`: drop `findAllForList` test if present.
- `AdminCatalogPageControllerTest`: assert only the `index` route seeds; remove `all-denoms` route assertions.
- Manual/browser: markup on checked rows → Harga Jual pending (dirty) → Simpan Harga; inline Nama edit → Simpan Nama; arrow-nav between Nama/Harga Jual across rows; unpriced banner + filter; existing Samakan/Sinkronkan/combobox still work; wide table scrolls without breaking the tabs.

## Risks / ceilings

- **Component size:** `index.html`'s Alpine grows notably (mixin + name-edit + markup + arrow-nav). Acceptable; the mixin extraction keeps price logic shared. `// ponytail: name-editing could also be extracted to a shared mixin like price-editing if a third page ever needs it; not now.`
- **Two selection models** (checkbox for markup, scope for Samakan) — intentional per user; label buttons clearly to avoid confusion.
- **Wide table** (checkbox + 2 inline-edit columns added) — horizontal scroll; if it feels cramped, drop low-value columns (Sort) — a build-time frontend-design call, not a spec requirement.
