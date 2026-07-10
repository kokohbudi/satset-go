# Handoff prompt — Catalog "Product = Brand × Category" (resume)

> Paste this whole file into a fresh Claude Code session in `/Users/kokohbudi/myProjects/satset-go` to continue.

## What we're building

Reseller must browse the catalog by service type: **Category → Brand → Denom**
(click "Paket Data" / "Game" / "Voucher" → pick operator → pick denom).

Digiflazz (DF) tags every price item `(category, brand, sku)`, and the same brand
appears under many categories (TELKOMSEL is in Pulsa, Data, Voucher, Aktivasi
Voucher…). The old model keyed a `products` row by a **global-unique `code`** in a
**single** category, so the first synced category claimed a brand and every later
category that shared it rendered "Belum ada produk".

**Chosen design (approved): Product = Brand × Category.** A `products` row is a brand
*within* a category. `products` uniqueness moved from `UNIQUE(code)` to
`UNIQUE(category_id, code)`; product lookups became per-category; sync reconcile is
category-scoped; storefront product/denom endpoints are category-scoped. Denoms are
unchanged and inherit category via their parent product.

- Spec: `docs/superpowers/specs/2026-07-10-catalog-brand-per-category-design.md`
- Plan: `docs/superpowers/plans/2026-07-10-catalog-brand-per-category.md`
- Ledger: `.superpowers/sdd/progress.md` (bottom section = this feature)
- Branch: `feat/catalog-brand-per-category` (branched from `main` @ `8f14800`)

## What is DONE (committed, green, reviewed)

All 4 implementation tasks done via subagent-driven development, commits
`8f14800..f68b1ea`:
- Task 1 `5f06ced`: `products` UNIQUE(category_id, code) — entity + dev-DB migration.
- Task 2 `dc91ede`+`e1fd11e`: `findByCodeAndCategoryId`, `existsByCodeAndCategoryIdAndIdNot`,
  `findByCategoryAndCode`, `findOrCreateByBrand(brand, categoryId)`; removed global
  `findByCode`; `previewProducts` existence check now per-category. (`e1fd11e` was a
  transitional `findFirstByCode` shim, **already deleted** in Task 4.)
- Task 3 `dc0433b`: `CatalogSyncService.reconcileForProduct` filters DF items by the
  product's **category AND brand**.
- Task 4 `f68b1ea`: storefront endpoints category-scoped —
  `GET /api/categories/{catCode}/products/{prodCode}` and `.../denoms`; old bare-code
  routes + `DenomDomainService.findByProduct` removed; `purchase/index.html` JS rewired;
  shim removed.

Verification: full `satset-core` suite **445/445 pass** (0 fail/0 err) at `f68b1ea`.
Final whole-branch review (opus): **READY TO MERGE**, no code-level blockers. Only
non-code gate: the prod DB migration must be run manually (see below). Also DataSeeder
was `git rm`'d earlier (catalog is now DF-sync-only).

## THE OPEN BUG — fix this first

Live verification is stuck. Setup that's already true:
- App is running on **:8080** via `mvn -pl satset-core spring-boot:run` (new build,
  PID changes; devtools active). System `mvn` only — **there is no `./mvnw`**.
- Dev DB `satset_go` catalog was **wiped**, then a `POST /api/admin/catalog/sync/all`
  was run from the logged-in browser.
- Result: **12 categories created, but `products = 0` and `product_denoms = 0`.**

Facts already established (don't re-derive):
- DF pricelist returns full data (see `satset-core/logs/supplier/supplier.log`), incl.
  brands under every category (e.g. Aktivasi Voucher → AXIS, XL; Voucher → TELKOMSEL).
- `products` constraint is correct: only `uq_products_category_code UNIQUE(category_id, code)`,
  old global unique gone.
- The 12 category `code`s match `CatalogCodeUtil.toCode(DF category)` exactly
  (PULSA, DATA, GAMES, EMONEY, PLN, GAS, VOUCHER, AKTIVASIVOUCHER, PAKETSMSTELPON,
  MASAAKTIF, AKTIVASIPERDANA, TV), so `previewProducts`' category filter should match.
- No error found in `logs/CatalogSyncService/CatalogSyncService.log` or
  `logs/omnip-services.log` for the sync (business logs route to the former via
  `@LogContext("CatalogSyncService")`).

Leading hypothesis: `syncAll` is **not atomic** (per-item commits) — `applyCategories`
commits the 12 categories, then something in the per-category product loop
(`previewProducts` → `applyProducts` → `findOrCreateByBrand`, or the denom
`reconcileForProduct` step) throws and aborts the rest, leaving products at 0. The
throw is not being surfaced in the logs checked. Need the actual `SyncResult` JSON
(added/updated/failed) or the real stack trace.

Concrete next steps:
1. Get the real failure signal. Either (a) capture the `SyncResult` JSON returned by
   `POST /api/admin/catalog/sync/all` (browser console:
   `fetch('/api/admin/catalog/sync/all',{method:'POST',headers:{'X-XSRF-TOKEN':document.cookie.match(/XSRF-TOKEN=([^;]+)/)[1]}}).then(r=>r.json()).then(console.log)`),
   or (b) mint a Keycloak token (realm `satset-go`, client `satsetgo-client` @
   `http://localhost:9999`) and curl it, or (c) tail the app's live stdout while the
   sync runs. `applyProducts` swallows per-item exceptions into a `failed` count and
   logs `"applyProducts gagal utk {}"` — check for that; if `failed>0`, the message +
   cause is the smoking gun.
2. If it's a thrown exception aborting `syncAll`, reproduce the specific call
   (`previewProducts(catId)` for one category, or `findOrCreateByBrand`) and read the
   trace. Suspects to check in `satset-core/.../supplier/service/CatalogSyncService.java`
   and `catalog/service/ProductDomainService.java`.
3. Fix, add/adjust a test (TDD), re-run `mvn -pl satset-core test -Dtest=CatalogSyncServiceTest`,
   commit on the branch.
4. Re-run `sync/all`, then verify:
   ```
   docker exec -i postgres-satset psql -U admin -d satset_go -c "
   SELECT c.name, count(p.id) prod FROM categories c LEFT JOIN products p ON p.category_id=c.id
   GROUP BY c.name ORDER BY prod;"
   ```
   Expect Voucher/Data/Aktivasi Voucher to have products, TELKOMSEL as a distinct row
   per category.

## Environment / conventions
- DB: `docker exec -i postgres-satset psql -U admin -d satset_go -c "..."` (the `-i` is
  required or heredoc/stdin is dropped).
- Keycloak: `http://localhost:9999`, realm `satset-go`, client `satsetgo-client`; app
  admin login is via browser (OAuth code flow).
- `graphify-out/graph.json` exists — run `graphify query "<q>"` to orient before
  grepping; `graphify update .` after code changes.
- TDD strict; tests: JUnit5 + AssertJ + Mockito; service tests are Mockito unit tests,
  repo/constraint tests are `@SpringBootTest(MOCK)` + `@Transactional` against dev PG.
- Sync endpoints (`CatalogSyncController`, `/api/admin/catalog`): `sync/all` (full),
  `categories/{id}/sync/products` (per-category, the admin "Sync Produk DF" button).

## After the bug is fixed — remaining decisions (ask the user)
- Finish the branch: merge to `main` locally, or push + PR? (Use
  superpowers:finishing-a-development-branch.)
- **Prod migration (required before deploy):** `ddl-auto=validate` does NOT check
  unique constraints, so if not run the feature silently fails at sync time. Run:
  ```sql
  ALTER TABLE products DROP CONSTRAINT <old UNIQUE(code) name>;  -- find via \d products
  ALTER TABLE products ADD  CONSTRAINT uq_products_category_code UNIQUE (category_id, code);
  ```
- Out of scope (do NOT treat as bugs): sell price/margin (DF sync writes `base_price`
  only, `price` is null → denoms not sellable until a separate margin task); hiding
  empty categories in UI; duplicated brand icon/name across a brand's per-category rows.
- MCP `postgres` server points at the old DB name `omni_pulsa` (renamed to `satset_go`)
  → its queries error; use the `docker exec` psql command instead, or fix the MCP config.
