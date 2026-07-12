# All-Denoms Tab + Markup Pricing — Design

**Date**: 2026-07-12
**Scope**: Halaman "Semua Denom" lintas produk di admin katalog + markup engine untuk isi harga jual (persen/fix, dari modal atau harga sekarang, ceil kelipatan 100). Build di atas fitur inline price edit (spec `2026-07-12-denom-inline-price-edit-design.md`).

## Problem

Isi/naikin harga jual denom saat ini harus per-produk (buka `denoms.html` tiap produk). Admin butuh: (1) satu tabel semua denom lintas produk, (2) cara massal isi harga jual pakai markup terhadap harga modal atau harga sekarang — tanpa ketik satu-satu.

## Decisions (dari brainstorm)

- **Base markup**: user pilih tiap apply — `Modal` (basePrice) atau `Harga Sekarang` (price). Kalau pilih `Harga Sekarang` tapi null → fallback ke `Modal`.
- **Tipe markup**: `Persen` (`base × (1 + p/100)`) ATAU `Fix Nominal` (`base + n`).
- **Rounding**: ceil ke kelipatan 100.
- **Scope apply**: filter (client) + checkbox select ("pilih semua yang tampil").
- **Preview**: reuse confirm modal yang sama dengan bulk price edit (per-denom `lama → baru`).
- **Simpan**: reuse endpoint `PUT /api/admin/catalog/denoms/prices` — TIDAK ada endpoint tulis baru.
- **No paging**: total denom saat ini 77; load-all + filter client. Paging ditunda sampai denom ribuan.

## Backend

### Repo query
`DenomRepository.findByDeletedFalseOrderByProductIdAscSortOrderAsc()` → semua denom non-deleted.

### DTO enrichment
Denom lintas produk perlu konteks. Tambah `categoryName` + `productName` ke response. Karena `ProductDenomDTO` existing dipakai endpoint lain, buat DTO khusus `DenomListItemDTO` (extends fields yang relevan + categoryName + productName) ATAU tambah dua field nullable ke `ProductDenomDTO` yang hanya diisi di path ini. **Keputusan**: DTO baru `DenomListItemDTO` — hindari polusi DTO existing.

Enrichment di service: sekali fetch semua Products + Category (katalog kecil), map productId → (productName, categoryName). Hindari N+1.

### Endpoint
```
GET /api/admin/catalog/denoms
@PreAuthorize hasRole('REALM_view_catalog')
Response: [ { id, code, name, denomType, nominal, price, basePrice, active, deleted,
              productId, productName, categoryName } ]
```
Simpan harga: reuse `PUT /api/admin/catalog/denoms/prices` (dari fitur sebelumnya).

### Page route
`AdminCatalogPageController`: `GET /admin/catalog/denoms` → `all-denoms.html`. SSR-seed initial list + kategori list untuk filter (pola SSR-first existing).

## Frontend — `all-denoms.html`

Komponen Alpine `allDenomManager()`.

### Kolom tabel
Kategori · Produk · Code · Nama · Nominal · Harga Modal · **Harga Jual (input inline)** · Status.

### Filter (client)
- Dropdown kategori (dari list kategori).
- Search produk (teks) + search umum (code/nama).
- Checkbox per row (non-deleted) + header "pilih semua yang tampil" (centang semua `filteredDenoms`).

### Markup bar
Base select (`Modal` / `Harga Sekarang`) · Tipe select (`Persen` / `Fix Nominal`) · input nilai (number) · tombol "Terapkan ke terpilih".

### Markup engine (client-side)
Untuk tiap baris terpilih:
```
base = (source === 'MODAL') ? d.basePrice : (d.price ?? d.basePrice)
if (base == null) → skip baris, kumpulkan ke daftar "dilewati"
raw  = (tipe === 'PERSEN') ? base * (1 + nilai/100) : base + nilai
newPrice = Math.ceil(raw / 100) * 100
```
Set `newPrice` ke `dirty` map (keyed `d.id`) — struktur sama dengan fitur inline edit. Setelah "Terapkan": kalau ada baris dilewati (base null), toast warning "N denom dilewati (tak ada harga modal/jual)". Tombol "Simpan Harga (N)" muncul → confirm modal (reuse) → `PUT /denoms/prices`.

### Banner unpriced
Reuse banner "belum ada harga jual" + auto-reset filter (dari fitur sebelumnya).

## Reuse / refactor

Logika price-edit inti dipakai `denoms.html` (per-produk) DAN `all-denoms.html`:
`dirty` map, `isDirty`/`pendingPrice`/`setPrice`/`resultFor`, `submitPrices`, confirm modal markup, `unpricedCount`/`filterUnpriced` + banner.

**Keputusan (pinned bentuk):**
- Logika JS shared → static file `satset-core/src/main/resources/static/js/denom-price-editing.js`, expose global factory `window.denomPriceEditing(getDenoms)` yang balikin objek state+method (dirty, setPrice, submitPrices, unpricedCount, dst.). Tiap halaman `<script src>` file ini lalu spread hasilnya ke komponen Alpine-nya. Plain JS, no Thymeleaf coupling, no build step.
- Confirm-modal markup (HTML) → Thymeleaf fragment `templates/fragments/denom-price-confirm-modal.html`, di-`th:replace` dua halaman.
- Halaman tetap punya bagian sendiri: `denoms.html` = CRUD/sync per-produk; `all-denoms.html` = markup + filter + checkbox lintas produk.

Refactor `denoms.html` ke shared fragment = bagian dari kerjaan ini (improve code yang disentuh), bukan refactor liar.

## Testing

- `DenomRepositoryTest` / `DenomDomainServiceTest`: query global non-deleted, enrichment productName/categoryName tanpa N+1 (verify satu fetch products).
- `AdminCatalogControllerTest`: `GET /denoms` mapped DTO + guard `view_catalog`.
- Markup engine (client JS): 1 self-check inline (assert ceil kelipatan 100, fallback base null skip, fallback price→modal) — atau langkah verifikasi manual browser di plan.

## Out of scope

- Paging (denom ribuan → tambah query+filter server-side).
- Create/delete/sync denom di tab ini — tetap per-produk di `denoms.html`.
- Markup engine server-side (semua client + reuse endpoint bulk existing).
- Simpan preset markup / riwayat.
