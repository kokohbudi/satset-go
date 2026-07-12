# Catalog Nested-Tab Redesign + Denom Reassign — Design

**Date:** 2026-07-12
**Status:** Approved (design), pending implementation plan
**Slice:** `catalog`

## Problem

The catalog admin UI is three separate full-page-load screens (`categories.html`,
`products.html`, `denoms.html`) with breadcrumb drill-down. Two pains:

1. **Too many clicks** to reach a denom (Kategori → Produk → Denom, each a page reload).
2. **Layout not clean** — link-based tabs + breadcrumbs, context lost on every navigation.

Separately, denoms today are **product-first**: a denom's owning product
(`ProductDenoms.productId`, NOT NULL) is set at create/sync time and **cannot be
changed** afterward. There is no way to reclassify a denom into a different
product/category once it exists.

## Goals

- Single page, nested client-side tabs (no reload) matching the data hierarchy
  Category → Product → Denom.
- A "Semua" (all) tab on the category level and the product level as a shortcut.
- Reuse the existing edit flows for Category and Product.
- **New:** allow reassigning an existing denom to a different product (category
  follows the product).

## Non-Goals (explicitly out of scope)

- Denom-first sync ingestion. DF (Digiflazz) sync stays **product-first** and
  unchanged (`CatalogSyncService.reconcileForProduct(productId)`, apply scoped to
  a productId).
- Orphan/staging/unassigned denoms. `ProductDenoms.productId` stays NOT NULL —
  every denom always belongs to exactly one product.
- Any change to CRUD/sync/search business logic beyond the denom-reassign field.

## Current State (verified)

- Templates: `satset-core/src/main/resources/templates/pages/admin/catalog/{categories,products,denoms}.html`,
  each a full page decorated by `layouts/base`, own Alpine `x-data`
  (`categoryManager()` / `productManager()` / `denomManager()`).
- Page controller: `AdminCatalogPageController` (`@RequestMapping("/admin/catalog")`)
  — routes `/categories`, `/products`, `/products/{id}/denoms`, SSR-seeds
  `initialCategories` / `initialProducts` / `initialDenoms` as JS globals.
- JSON API: `AdminCatalogController` + `ProductCatalogController` under
  `/api/admin/catalog/...` (CRUD + sync), consumed by Alpine `fetch()`.
- Model: `Category` 1:N `Products` (has `categoryId` UUID) 1:N `ProductDenoms`
  (has `productId` UUID, NOT NULL). Raw UUID FKs, no JPA object graph.
- Edit Category — EXISTS (`PUT /api/admin/catalog/categories/{id}`, modal in
  `categories.html`).
- Edit Product incl. move category — EXISTS (`PUT .../products/{id}`,
  `ProductDomainService.update` sets categoryId; `products.html` category select).
- Edit Denom — EXISTS but **cannot change productId** (`UpdateDenomRequest` has no
  productId, `DenomDomainService.update` never touches it).

## Design

### 1. Single-page nested tabs

Replace the three pages with **one** page served at `/admin/catalog` (redirect
target changes from `/categories` to the page itself). One Alpine component
`catalogManager()`.

Layout:

```
[Semua] [PULSA] [DATA] [GAME] [PLN]        ← tab kategori   (activeCategory: id | null)
[Semua] [TELKOMSEL] [XL] [INDOSAT]         ← subtab produk  (activeProduct:  id | null)
──────────────────────────────────────────
<panel>  [+ Sync DF] [+ Denom] / [+ Produk] / [+ Kategori]
```

State (Alpine):
- `activeCategory` — category id, or `null` = "Semua".
- `activeProduct` — product id, or `null` = "Semua".

Panel content rules:
- `activeCategory = null` (Semua) → panel = flat table of **all products** (across
  categories). Product subtab bar hidden or shows nothing selectable.
- `activeCategory = <id>`, `activeProduct = null` (Semua) → panel = flat table of
  **all denoms in that category** (across its products).
- `activeCategory = <id>`, `activeProduct = <id>` → panel = **denoms of that
  product**.

All switching is client-side; no page reload.

### 2. Data loading

- SSR-seed `categories` + `products` (full lists, small) as JS globals on first
  paint — consistent with the project's SSR-first convention.
- Denoms are **lazy-fetched per product** on first time that product (or its
  category's "Semua") is viewed, then cached in the Alpine component. Reuse the
  existing denom fetch endpoint. Avoids dumping every denom up front.
- "Semua denom in category" view fetches denoms for each product in that category
  (or a category-scoped denom fetch) and caches.

### 3. Edit placement (reuse existing modals)

- **Edit Kategori** — reuse the existing `categories.html` edit modal + its
  `PUT /api/admin/catalog/categories/{id}` call. Trigger lives near the category
  tab bar (e.g. a "Kelola Kategori" affordance / edit icon on the active category).
- **Edit Produk** (incl. move to another category) — reuse the existing
  `products.html` edit modal + `PUT .../products/{id}`. Trigger near the product
  subtab / product row.
- The three per-page Alpine components collapse into `catalogManager()`; their
  modal markup + fetch calls are carried over, not rewritten.

### 4. NEW — reassign denom

Allow moving an existing denom to a different product (its category follows the
chosen product, since a denom has no direct categoryId).

Backend:
- `UpdateDenomRequest` — add `@NotNull UUID productId`.
- `DenomDomainService.update(id, req)` — set `productId` after validating the
  target product exists (throw the catalog's existing not-found exception if not).
- No new endpoint — the existing `PUT /api/admin/catalog/denoms/{id}` carries it.

UI:
- Denom edit modal gains a **cascade selector**: pick Kategori → filter Produk →
  pick Produk. The denom's new `productId` = chosen product. Category is implied,
  not sent.

### 5. Testing (TDD)

- Red→green test: `DenomDomainService.update` changes `productId` to a valid
  target product; and rejects a non-existent target product.
- Existing catalog tests must stay green (no regressions to CRUD/sync/search).

## Data Flow (after)

Controller SSR-seeds categories+products → `catalogManager()` seeds state → tab
clicks filter client-side → denom lists lazy-fetched + cached via existing REST →
edits (category/product/denom incl. reassign) go through existing PUT endpoints;
on denom reassign the denom moves between product/category tabs by re-filtering
(no reload).

## Files (anticipated)

- `templates/pages/admin/catalog/index.html` — new single page (absorbs the three).
- `AdminCatalogPageController` — collapse to one page route; keep SSR seeding.
- `UpdateDenomRequest`, `DenomDomainService` — add productId reassign.
- Remove/retire `categories.html`, `products.html`, `denoms.html` (via `git rm`)
  once markup is carried into `index.html`.
