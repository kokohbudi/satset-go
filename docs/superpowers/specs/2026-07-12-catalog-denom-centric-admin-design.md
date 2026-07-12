# Catalog Admin: Denom-Centric Single Page — Design

**Date:** 2026-07-12
**Branch:** `worktree-catalog-nested-tab`
**Scope:** Admin catalog page only (`pages/admin/catalog/index.html` + supporting backend). Buyer catalog, supplier/DF sync, transaction flow, and the `Category`/`Products`/`ProductDenoms` schema are **untouched**.

## Goal

Make the denom the single unit of work on the admin catalog page. The denom table is always the content, filtered by two levels of tabs (category, product). Category and product remain real entities (kept for buyer catalog + provider sync + icons), but on this page they behave like fields of a denom: created inline from the denom modal, edited from their tab pill, and shown as tabs only when they actually hold denoms.

### Behavior the user asked for

- Category = Semua, Product = Semua → **all denoms**.
- Category = A, Product = Semua → **all denoms whose product is in category A**.
- Category = A, Product = B → **denoms of product B**.

## Non-goals

- No schema change. `Category`, `Products`, `ProductDenoms` entities and tables stay as-is.
- No change to buyer catalog (`ProductCatalogController`), purchase page, or supplier sync (`CatalogSyncService`).
- No new transactional endpoint (client orchestrates existing POSTs).

---

## Backend

All in the `catalog` slice.

### 1. `DenomRepository`
Add:
```java
List<ProductDenoms> findAllByOrderBySortOrder();
```

### 2. `DenomDomainService`
Add:
```java
public List<ProductDenoms> findAllForAdmin() {
    return denomRepository.findAllByOrderBySortOrder(); // includes deleted; admin view greys them out
}
```

### 3. `AdminCatalogController`
Add aggregate read (view permission, same as per-product listing):
```java
@GetMapping("/denoms")
@PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
public ResponseEntity<List<ProductDenomDTO>> listAllDenoms() {
    List<ProductDenomDTO> dtos = manageDenomsUseCase.findAllForAdmin().stream()
            .map(CatalogDtoMapper::toDenomDTO).toList();
    return ResponseEntity.ok(dtos);
}
```
No new DTO fields. `ProductDenomDTO.productId` already carries the link; the client resolves product → category names from the `initialProducts` / `initialCategories` maps it already holds.

### 4. `AdminCatalogPageController`
Seed all denoms for instant first paint (SSR-first house pattern):
```java
List<ProductDenomDTO> denoms = manageDenomsUseCase.findAllForAdmin().stream()
        .map(CatalogDtoMapper::toDenomDTO).toList();
model.addAttribute("initialDenoms", denoms);
```
Keep existing `initialCategories`, `initialProducts`, `categoryTypes`, `denomTypes`.

### Reused as-is (no change)
- `POST /categories`, `POST /products` — used by the combobox inline-create orchestration.
- `POST /products/{id}/denoms`, `PUT /denoms/{id}`, `DELETE /denoms/{id}` — denom CRUD.
- `PUT /categories/{id}`, `PUT /products/{id}`, `DELETE /categories/{id}`, `DELETE /products/{id}` — tab-pill edit/delete.
- `GET /products/{id}/pricelist-compare`, `POST /products/{id}/sync/denoms` — DF compare/sync (per-product only).

---

## Frontend (`pages/admin/catalog/index.html`)

### State (`catalogManager()`)
- Add `denoms: INITIAL_DENOMS` (seeded).
- Keep `categories`, `products`, `activeCategory`, `activeProduct`.
- Remove per-product denom cache as the primary source; `denoms` is now the single denom array. (`compareByProduct` for DF stays, loaded only in single-product scope.)

### Data flow
- **First paint:** render from seeded `denoms`, scope Semua/Semua → all denoms.
- **On tab toggle** (`selectCategory` / `selectProduct`): refetch `GET /api/admin/catalog/denoms`, replace `denoms` (fresh prices), then filter client-side by scope. Spinner acceptable on the table region during refetch.
- **After denom create/edit/delete:** refetch `GET /denoms` so tab auto-prune and rows stay correct.

### Scope filter (getter `filteredDenoms`)
```
base = denoms
if activeProduct != null      → base.filter(d => d.productId === activeProduct)
else if activeCategory != null→ base.filter(d => productCategoryId(d.productId) === activeCategory)
else                          → base            // Semua / Semua
then apply searchQuery (name/code/type/product name/category name)
```
`productCategoryId(pid)` = `products.find(p => p.id === pid)?.categoryId`.

### Denom table
- Always visible. **Remove the products-table panel entirely.**
- New columns **Kategori** and **Produk** (resolved from `d.productId` via the product/category maps).
- Keep existing columns incl. `Harga DF` (see gating).
- Deleted rows greyed (existing pattern).

### Tabs = filter + auto-prune
- `denomCategoryIds` = set of `productCategoryId(d.productId)` over active, non-deleted denoms.
- `denomProductIds` = set of `d.productId` over active, non-deleted denoms.
- Category chips: `categories` filtered to `denomCategoryIds`. Product chips: `productsInCategory` filtered to `denomProductIds`.
- "Semua" chip always present at each level.
- Empty categories/products stay in DB and in the combobox source; they are simply not rendered as chips.

### Tab pill = edit existing entity
- Pencil on the **active** category chip → opens the (edit-only) category modal for that category.
- Pencil on the **active** product chip → opens the (edit-only) product modal for that product.
- These modals keep their icon / type / provider / **Hapus** controls (delete lives here).
- **Remove** the `+ Kategori` and `+ Produk` create buttons — creation moves to the denom modal.

### Denom modal = single create door (denom + inline category + product)
Replaces the old reassign cascade. Layout inside the modal:

- **Kategori** — combobox (datalist-backed text input or Alpine dropdown):
  - Pick existing (by name) OR type a new name.
  - When the typed value matches no existing category → reveal required **Tipe** select (`PREPAID` / `POSTPAID`) + optional **Icon URL**.
- **Produk** — combobox:
  - Pick existing OR type new.
  - When new → reveal optional **Provider** + **Icon URL**. Product `code` auto-derived from name (uppercase/slug); denom `code` blank → existing auto-gen.
- Then the existing denom fields (pricing, detail, status).

**Save orchestration** (reuse existing endpoints, client-side):
1. If category is new → `POST /categories` `{code:auto, name, categoryType, iconUrl}` → get id.
2. If product is new → `POST /products` `{categoryId, code:auto, name, providerName, iconUrl}` → get id.
3. `POST /products/{productId}/denoms` (create) or `PUT /denoms/{id}` (edit; productId from combobox selection → moves the denom).
4. Refetch categories/products/denoms; toast success.

`// ponytail: 3-step client orchestration; a mid-step failure can leave an orphan category/product. Admin tool, surfaced via toast. Promote to one transactional endpoint only if orphans become a real problem.`

### Gated by scope (need a concrete productId)
- **`Sync Denom DF`** button and **`Harga DF`** column: shown/populated only when a single product is selected (`activeProduct != null`); `compareByProduct` loaded for that product only, as today. Aggregate scope shows `-`.
- **`Tambah Denom`**: always available; product is chosen via the combobox in the modal.

### Search
- Always denom search. Drop the product/denom placeholder switch; placeholder = `Cari denominasi...`.

### Removed
- Products-table panel; `+ Kategori` / `+ Produk` create buttons; the product/denom search-placeholder switch; the old reassign cascade block (superseded by the combobox).

---

## Testing

- `AdminCatalogControllerTest`: `GET /denoms` returns all denoms (incl. deleted) for a `view_catalog` user; 403 without the role.
- `DenomDomainServiceTest`: `findAllForAdmin()` returns repository order.
- Manual / browser: the three scope cases (Semua/Semua, A/Semua, A/B); auto-prune (delete a product's last denom → chip disappears after refetch); combobox create (new category+product+denom in one save); tab-pill edit; DF column dashes in aggregate and populates in single-product.

## Risks / ceilings

- **Payload:** all denoms seeded + refetched per toggle. Fine while total denoms are modest; if it grows large, switch the endpoint to a scoped `?categoryId=&productId=` variant (tabs would then need a separate lightweight presence source). Noted, not built.
- **Orphan on partial create failure** — see ponytail note above.
- **Rename of an existing category/product** touches its own row only (still a real entity), so no bulk update — unlike the abandoned flatten approach.
