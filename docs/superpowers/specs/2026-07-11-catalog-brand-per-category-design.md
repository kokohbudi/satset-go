# Design — Catalog "Product = Brand × Category"

**Date:** 2026-07-11
**Branch:** `feat/catalog-brand-rework` (from `main` @ `2d06159`)
**Status:** Approved, ready for plan

## Problem

A `products` row is keyed by a **globally-unique `code`** (`Products.code @Column(unique = true)`).
Digiflazz (DF) tags every price item `(category, brand, sku)`, and the **same brand
appears under many categories** — TELKOMSEL is in Pulsa *and* Data (and Voucher,
Aktivasi Voucher…). Under a global-unique code, the first synced category claims
TELKOMSEL and every later category that shares it renders "Belum ada produk", because
`findOrCreateByBrand` finds the existing TELKOMSEL in *any* category and never creates
the per-category row.

## Chosen design — Approach A (denormalized: Product = Brand × Category)

A `products` row is a **brand within a category**. Uniqueness moves from
`UNIQUE(code)` to `UNIQUE(category_id, code)`. Product lookups become category-scoped;
sync reconcile is already category-scoped; storefront product/denom endpoints become
category-scoped. Denoms are unchanged and inherit category via their parent product.

```
products
  id | category_id | code      | name      | icon_url
  1  | PULSA       | TELKOMSEL | TELKOMSEL | tsel.png
  2  | DATA        | TELKOMSEL | TELKOMSEL | tsel.png   <- duplicate icon/name (accepted)
UNIQUE(category_id, code)
product_denoms.product_id -> products.id   (per-category, unchanged)
```

Rejected: **Approach B (normalized Brand table)** — single-source brand metadata but a
new table, a join, and a heavier migration. Not worth it; `products` already carries
`category_id`, so A is the smaller change. Duplicated brand icon/name across a brand's
per-category rows is accepted (out of scope).

## Changes

### Entity — `Products`
- Drop `@Column(unique = true)` on `code`.
- Add table-level constraint:
  `@Table(name = "products", uniqueConstraints = @UniqueConstraint(name = "uq_products_category_code", columnNames = {"category_id", "code"}))`.

### Repository — `ProductRepository`
- Remove global `findByCode(String)`.
- Remove `existsByCodeAndIdNot(String, UUID)`.
- Add `findByCategoryIdAndCode(UUID categoryId, String code)`.
- Add `existsByCategoryIdAndCodeAndIdNot(UUID categoryId, String code, UUID id)`.
- Add `findByCategory_CodeAndCode(String categoryCode, String code)` — storefront lookup
  by human category code + brand code (derived-query nav through the `Category` relation,
  or a `@Query` join if the FK is a raw `categoryId` column rather than a mapped relation;
  the plan resolves which).

### Service — `ProductDomainService`
- `findOrCreateByBrand(brand, categoryId)` — lookup `(categoryId, code)` instead of global
  `findByCode(code)`. **Core fix.** Revive soft-deleted per-category row as before.
- `findByCode(code)` (storefront) → `findByCategoryAndCode(categoryCode, prodCode)`.
- `create` dup-check → category-scoped (`findByCategoryIdAndCode`).
- `update` dup-check → `existsByCategoryIdAndCodeAndIdNot`.
- `reconcileSupplierFlags(categoryId, codes)` — already category-scoped, no change.

### Sync — `CatalogSyncService`
- `previewProducts` existence check (line ~102) → `findByCategoryAndCode` instead of
  global `findByCode`.

### Denoms — `DenomDomainService`
- `findByProduct(productCode)` (storefront denom browse) → category-scoped:
  `findByCategoryAndProduct(categoryCode, prodCode)`. Denom model unchanged.

### Storefront — `ProductCatalogController`
- Replace `GET /products/{code}` → `GET /categories/{catCode}/products/{prodCode}`.
- Replace `GET /products/{code}/denoms` → `GET /categories/{catCode}/products/{prodCode}/denoms`.
- Remove bare `/products/{code}` and `/products/{code}/denoms` routes.
- Rewire `purchase/index.html` JS to the nested URLs.

### Dev seed — `DataSeeder`
- `git rm` DataSeeder.java. Catalog is DF-sync-only; the seeder's global `findByCode`
  collides with the new constraint and it is no longer needed.

### Migration
- Dev DB (`ddl-auto=update` does NOT drop the old unique automatically):
  ```sql
  ALTER TABLE products DROP CONSTRAINT <old UNIQUE(code) name>;   -- find via \d products
  ALTER TABLE products ADD  CONSTRAINT uq_products_category_code UNIQUE (category_id, code);
  ```
  via `docker exec -i postgres-satset psql -U admin -d satset_go -c "..."`.
- Prod DB (`ddl-auto=validate` does NOT check unique constraints → feature silently fails
  at sync time if not run): same ALTERs, run manually before deploy.

## Out of scope (not bugs)
- Duplicated brand icon/name across a brand's per-category rows.
- Sell price/margin (DF sync writes `base_price` only; `price` null → not sellable yet).
- Hiding empty categories in UI.

## Testing (TDD strict)
- Service tests: Mockito unit (`ProductDomainServiceTest`, `CatalogSyncServiceTest`,
  `DenomDomainServiceTest`).
- Constraint test: `@SpringBootTest(MOCK)` + `@Transactional` vs dev PG — same brand code
  in two categories succeeds; same brand code twice in one category violates
  `uq_products_category_code`.
- Controller test: nested route returns the correct per-category product/denoms.
- Verify: full `satset-core` suite green before finishing branch.

## Live verification (after green)
Wipe dev catalog, run `POST /api/admin/catalog/sync/all`, then:
```sql
SELECT c.name, count(p.id) prod FROM categories c
LEFT JOIN products p ON p.category_id = c.id GROUP BY c.name ORDER BY prod;
```
Expect Voucher/Data/Aktivasi Voucher to have products, TELKOMSEL a distinct row per category.
