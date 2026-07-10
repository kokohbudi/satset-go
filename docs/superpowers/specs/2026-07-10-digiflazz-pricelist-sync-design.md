# Design: Digiflazz Price-List Sync ke Katalog

**Tanggal:** 2026-07-10
**Branch:** feat/fly-outbound-proxy
**Status:** disetujui (brainstorm), belum diimplementasi

## Context

Admin butuh nyelaraskan katalog lokal dengan daftar-harga Digiflazz (DF). Slice
sebelumnya (sudah jadi, unmerged) cuma **compare read-only**: `DigiflazzClient.fetchPriceList()`
→ `PriceReconcileService.reconcile()` → tabel banding harga beli DB (`ProductDenoms.basePrice`)
vs harga DF (`price`) per SKU, status SAMA/NAIK/TURUN/BARU/HILANG di `/admin/pricelist`.

Sekarang admin mau **aksi selaraskan**: tombol yang bikin/ubah katalog biar match DF.

### Konsep SKU (terverifikasi via doc resmi DF)

`buyer_sku_code` = "Kode produk yang disetting oleh Anda sebagai Buyer" — kode milik
akun buyer, **stabil**, unik di akun kita. Seller di belakang layar diurus routing DF;
beda seller **tidak** bikin denom baru. Maka `denom.code == buyer_sku_code` (1:1) valid.
`seller_name` di response live **di-mask** (`Ki***`) → cosmetic, bukan data.

## Keputusan (locked)

| # | Keputusan |
|---|---|
| Match key | `denom.code == buyer_sku_code`, case-insensitive utk compare |
| SKU case saat create | simpan **apa adanya** (lowercase, mis. `dana20`) — beda dari `create()` manual yg uppercase. Alasan: `RealProviderAdapter` kirim `denom.code()` ke DF; DF SKU lowercase |
| Scope sync | full mirror: BARU→create, NAIK/TURUN→update cost, HILANG→nonaktif |
| Granularitas | per-baris **dan** "Selaraskan Semua" |
| Induk hilang | auto-create rantai Category→Product→Denom dari nama DF |
| seller_name | tidak disimpan di entity; kolom hint di tabel saja |
| Server trust | sync **recompute reconcile fresh** di server, tidak percaya harga dari client |

## Arsitektur (Approach A)

Orchestrator di transaction slice; tulis tetap lewat service catalog (boundary bersih,
supplier client tetap di transaction). ModularityTest: transaction→catalog.service (bukan repo).

### Catalog slice — method baru

- `CategoryDomainService.findOrCreateByName(String dfName) → Category`
  cari `toCode(dfName)`; absen → create `{code, name=dfName, categoryType=PREPAID, active=true}`.
- `ProductDomainService.findOrCreateByBrand(String brand, UUID categoryId) → Products`
  cari `toCode(brand)`; absen → create `{code, name=brand, categoryId, active=true}`.
- `DenomDomainService.createFromSupplier(UUID productId, String sku, String name, BigDecimal cost) → ProductDenoms`
  create `{code=sku (TIDAK di-uppercase), name, productId, denomType=FIXED_DENOM, basePrice=cost, price=null, active=true, deleted=false}`.
  Method **baru** — `create()` manual (uppercase) tidak diubah.
- `DenomDomainService.updateCostById(UUID denomId, BigDecimal cost)` — NAIK/TURUN: set `basePrice`, save.
- `DenomDomainService.deactivateById(UUID denomId)` — HILANG: `active=false`, save.
  *(pakai id, bukan code: denom lama bisa UPPERCASE, DF sku lowercase → lookup by code ambigu.)*
- `toCode(String name)` (helper, mis. di `CatalogDtoMapper` atau util kecil): uppercase +
  buang non-alfanumerik. "E-Money"→`EMONEY`, "Mobile Legends"→`MOBILELEGENDS`.
  *(ponytail: normalisasi kasar, bisa nabrak antar-nama; jarang, admin bisa rename.)*

### Transaction slice

- `PriceSyncService`:
  - `SyncResult syncAll()` — `reconcile()` fresh → loop → `apply(row)` → tally.
  - `SyncResult syncOne(String sku)` — `reconcile()` fresh → filter 1 sku (utk HILANG cocokkan
    by denom code) → `apply`.
  - `apply(PriceCompareRow)`:
    - BARU → `cat=findOrCreateByName(row.category)`; `prod=findOrCreateByBrand(row.brand, cat.id)`;
      `createFromSupplier(prod.id, row.buyerSku, row.productName, row.dfCost)`.
    - NAIK/TURUN → `updateCostById(row.denomId, row.dfCost)`.
    - HILANG → `deactivateById(row.denomId)`.
    - SAMA → skip.
  - per-item try/catch (gagal 1 → tally `failed`, batch lanjut). Log `e`, **tidak** expose message.
- `SyncResult` record: `{int created, costUpdated, deactivated, skipped, failed}`.
- `AdminPriceListController` (REST, `/api/admin/pricelist`, gated `manage_denoms`):
  - `POST /sync` → `syncAll()` → `SyncResult` JSON.
  - `POST /sync/{sku}` → `syncOne(sku)` → `SyncResult`.
  - CSRF auto via `base.html` fetch wrapper.

### Perubahan model / robustness

- `PriceListItem`: tambah `@JsonProperty("seller_name") String sellerName`; ubah `stock` dari
  `int` → `String` (doc DF: stock String; hindari parse error prod).
- `PriceCompareRow`: tambah `String seller` (hint) + `UUID denomId` (null utk BARU; dipakai
  apply NAIK/TURUN/HILANG biar tak re-lookup by code). `ProductDenoms.price` kolom nullable →
  `createFromSupplier` boleh set `price=null`.
- `PriceReconcileService`: guard SKU dobel di list DF → ambil harga terendah (rare, defensif).

### UI (`pages/admin/pricelist.html`)

- Alpine `priceListManager()` seeded dari SSR `rows`; hitung count per status; state `saving`.
- Atas: **"Selaraskan Semua"** → `Alpine.store('confirm')` (ringkas: X buat, Y update, Z nonaktif)
  → `fetch POST /api/admin/pricelist/sync` → `Alpine.store('toast')` summary → reload.
- Per-baris: tombol **"Selaraskan"** (tampil kecuali SAMA) → `fetch POST .../sync/{sku}` → toast → reload.
- Kolom baru: `Seller` (hint masked).

## Data flow

Tiap sync re-fetch DF + reconcile ulang → apply harga terkini (bukan SSR basi). `syncOne`
recompute lalu filter 1 sku. DF ~77 item, 1 call/klik — murah.

## Error handling

Per-item independen (`@Transactional` per method catalog). Gagal 1 item → tally, lanjut.
DF fetch mati total → controller balikin error, UI toast. Log-only, no leak.

## Testing (TDD, Red→Green)

- Catalog: `findOrCreateByName`/`findOrCreateByBrand` (create vs existing + `toCode` normalize);
  `createFromSupplier` **assert code TIDAK uppercase**; `updateCostById`; `deactivateById`;
  `toCode()` unit ("E-Money"→EMONEY dll).
- `PriceSyncServiceTest` (mock catalog services + DigiflazzClient/reconcile): BARU→create chain
  (verify urutan panggilan), NAIK/TURUN→updateCost, HILANG→deactivate, SAMA→skip, tally count,
  `syncOne` filter.
- `AdminPriceListControllerTest`: endpoint→service, security, SyncResult JSON.
- Update `PriceReconcileServiceTest`: seller hint + guard SKU dobel.
- Regression: `ModularityTest` (boundary transaction→catalog.service).

## Verifikasi end-to-end

1. `mvn -Dtest='PriceSyncServiceTest,PriceReconcileServiceTest,AdminPriceListControllerTest,AdminPriceListPageControllerTest,DigiflazzClientTest,ModularityTest' test` hijau.
2. Boot app (`.env` ada DIGIFLAZZ_* + PROXY_PASS), buka `/admin/pricelist`:
   - klik "Selaraskan" 1 baris BARU → cek denom kebentuk (`docker exec postgres-satset psql ...`),
     `code` lowercase, `basePrice` = harga DF, `price` null.
   - klik "Selaraskan Semua" → toast ringkasan; ulang reconcile → mayoritas jadi SAMA.
   - baris HILANG → denom `active=false`.

## Out of scope (slice berikut)

- Postpaid (`cmd:pasca`) — schema beda (`admin`, `commission`).
- Field `start_cut_off`/`end_cut_off` (jam gangguan), `type` (varian), `multi`.
- Auto-set aktif dari `seller_product_status && buyer_product_status`.
- Harga jual (`price`) otomatis — admin isi manual ("cukup set harga jual saja").
- Sidebar nav link (KC role attr) ke `/admin/pricelist`.

---

## REVISI 2026-07-10 — Sync per-level di halaman katalog (menggantikan §UI + §Endpoint di atas)

Keputusan baru: tombol sync **pindah ke halaman katalog per-level**; halaman standalone
`/admin/pricelist` **dibuang** (hapus `AdminPriceListPageController`, `pricelist.html`,
testnya, dan `PriceReconcileService.reconcile()` whole-catalog + `DenomDomainService.findAllActive`
kalau jadi tak terpakai).

**Perilaku per-level:**
- **Halaman Kategori** (`/admin/catalog/categories`): tombol **Sync Kategori** → create-missing
  Category dari daftar kategori DF (distinct `category` → `findOrCreateByName`).
- **Halaman Produk** (`/admin/catalog/products?categoryId=`): tombol **Sync Produk** → create-missing
  Product (brand) DF **dalam kategori yang sedang dibuka** (`toCode(row.category)==category.code`
  → distinct `brand` → `findOrCreateByBrand`).
- **Halaman Denom** (`/admin/catalog/products/{id}/denoms`): tombol **Sync Denom** → **full mirror**
  khusus SKU brand produk itu (`toCode(row.brand)==product.code`): BARU→create, NAIK/TURUN→update
  cost, HILANG→nonaktif. **+ kolom delta DF** per denom (harga beli DB vs DF) via GET compare.

**Service** `CatalogSyncService` (transaction slice, inject DigiflazzClient + 3 catalog service):
`SyncResult syncCategories()`, `syncProducts(UUID categoryId)`, `syncDenoms(UUID productId)`,
`List<PriceCompareRow> reconcileForProduct(UUID productId)` (buat kolom delta).

**Endpoint** `CatalogSyncController` (transaction/web, gated manage roles, CSRF auto):
- `POST /api/admin/catalog/sync/categories`
- `POST /api/admin/catalog/categories/{categoryId}/sync/products`
- `POST /api/admin/catalog/products/{productId}/sync/denoms`
- `GET  /api/admin/catalog/products/{productId}/pricelist-compare` → `List<PriceCompareRow>`
  (denom page fetch client-side buat delta — hindari SSR catalog.web→transaction.service).

**Mapping (join key):** Category.code = `toCode(DF category)`, Product.code = `toCode(DF brand)`,
Denom.code = `buyer_sku_code` (lowercase). Sync mengalir kategori→produk→denom jadi code konsisten.

**Caveat (didokumentasikan, tidak diblokir):**
- Kategori/Produk legacy dengan code ≠ `toCode(nama DF)` (mis. "GAME" vs DF "Games"→`GAMES`)
  → dianggap beda, bisa muncul duplikat; admin merge manual.
- `syncDenoms` = mirror penuh: denom manual di bawah produk yang **tidak** ada di DF akan
  **dinonaktifkan** (HILANG). Jangan campur denom manual di produk yang di-sync DF.

---

## REVISI 3 2026-07-10 — Preview + apply selektif + tawaran hapus (menggantikan flow one-click)

Flow lama (klik → langsung tulis semua) diganti: **klik → preview modal (checkbox) → confirm → apply cuma yang dipilih**. Berlaku 3 level.

Keputusan (locked): (a) checkbox **selektif** per item; (b) **check-all** per grup; (c) grup **Hapus** default checkbox **OFF** (opt-in, cegah kehapus gak sengaja); (d) "hapus" = **soft-delete** (`deleted=true, active=false`) via `softDelete` existing; (e) server **recompute diff fresh** saat apply (harga dari server, cuma terima daftar KEY terpilih dari client).

**Model baru** (`transaction/model`): `enum SyncAction { ADD, UPDATE, DELETE }`; `record SyncPreviewItem(SyncAction action, String key, String label, String detail)`. `SyncResult` di-rename → `(int added, int updated, int deleted, int skipped, int failed)`.

**Preview per level** (grup: ➕Tambah ADD / ✏️Update UPDATE / 🗑️Hapus DELETE):
- Kategori: ADD = kategori DF belum ada; DELETE = kategori katalog (non-deleted) yg code-nya gak ada di DF.
- Produk (per kategori): ADD = brand DF belum ada (global `findByCode`); DELETE = produk di kategori itu yg gak ada di DF.
- Denom (per produk): pakai `reconcileForProduct` existing — BARU→ADD, NAIK/TURUN→UPDATE, HILANG→DELETE, SAMA→disaring.

**Service** (`CatalogSyncService`, ganti 3 method all-apply): `previewCategories()`, `applyCategories(List<String> keys)`, `previewProducts(UUID)`, `applyProducts(UUID, List<String> keys)`, `applyDenoms(UUID productId, List<String> selectedSkus)`; `reconcileForProduct` tetap (sumber preview denom + kolom delta). Denom HILANG apply = `denomService.softDelete` (bukan `deactivate`). Key: ADD=nama/brand/sku, UPDATE=sku denom, DELETE=UUID entity; server recompute preview, apply item yg key ∈ selected.

**Endpoint** (`CatalogSyncController`): `GET /sync/categories/preview`, `GET /categories/{id}/sync/products/preview`, `GET /products/{id}/pricelist-compare` (existing, sumber preview denom); apply `POST` sekarang terima `@RequestBody List<String>` (key terpilih).

**UI** (3 template katalog): tombol Sync → fetch preview → modal DaisyUI (grup ADD/UPDATE/DELETE, checkbox per item, check-all per grup, DELETE default OFF) → confirm → POST key terpilih → toast(added/updated/deleted/failed) → reload.

**Tetap caveat:** apply per-item commit (gak atomik), gak ada undo setelah confirm. Kategori/produk/denom manual muncul di grup Hapus (default OFF, user yg mutusin).
