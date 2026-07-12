# Denom Inline Price Edit — Design

**Date**: 2026-07-12
**Scope**: Admin catalog denoms page — inline edit harga jual (`price`) dengan bulk submit + confirmation modal.

## Problem

Edit harga jual denom saat ini lewat modal full-form per denom (`openEditModal` → `PUT /api/admin/catalog/denoms/{id}`). Update harga banyak denom sekaligus (mis. setelah DF sync, semua price masih null) butuh N kali buka-modal — lambat.

## Decisions (dari brainstorm)

- Field editable: **`price` saja**. Field lain tetap read-only di tabel (modal existing tetap ada untuk full edit).
- SKU (`code`) **read-only** — DF-owned (`buyer_sku_code`), dipakai transaksi, edit di DF dashboard.
- Confirmation: **modal** (bukan halaman terpisah) — diff review sebelum submit, hasil per-denom sesudah submit.

## UI — `denoms.html`

1. Kolom "Harga Jual": tampil `<input type="number">` inline per row (non-deleted; denom inactive tetap editable — set harga dulu sebelum diaktifkan). Perubahan tercatat di Alpine state `dirty` map: `{id → {code, name, oldPrice, newPrice}}`. Revert ke nilai awal = keluar dari dirty.
2. Tombol **"Simpan Perubahan (N)"** muncul saat `dirty` non-empty (di samping tombol Tambah).
3. Klik → **confirmation modal**: tabel diff `code · name · harga lama → harga baru`. Tombol Batal / Konfirmasi.
4. Konfirmasi → satu request bulk. Hasil per-denom ditampilkan di modal yang sama (badge sukses/gagal + pesan error), lalu `loadDenoms()` refresh tabel. Dirty entries yang sukses di-clear; yang gagal tetap dirty.

## Backend

Endpoint baru di `AdminCatalogController`:

```
PUT /api/admin/catalog/denoms/prices
@PreAuthorize hasRole('REALM_manage_denoms')  // sama dengan endpoint denom lain
Body:    [ { "id": "<uuid>", "price": 1500.00 }, ... ]
Response: [ { "id": "...", "code": "byu10", "ok": true },
            { "id": "...", "code": "flash1", "ok": false, "error": "..." } ]
```

Service: `DenomDomainService.updatePrices(List<PriceUpdate>)`:
- Loop per item: load denom (not found → item error), validasi `price > 0` di service (bukan bean validation — satu item invalid tidak boleh 400-kan seluruh batch, cukup item error), set price, save.
- Validasi (not found / harga ≤ 0 / deleted) → per-item error, dicek sebelum save.
- Persistensi: **satu transaksi** (`@Transactional` method-level, konsisten dengan method write lain di service). Optimistic lock conflict (edit bersamaan, langka) → seluruh batch batal, UI retain dirty state → user retry via tombol Konfirmasi.

DTO baru: `BulkPriceUpdateRequest` record `(UUID id, BigDecimal price)` + response record `(UUID id, String code, boolean ok, String error)`.

## Kenapa bulk endpoint (bukan N× PUT existing)

`UpdateDenomRequest` wajib full payload (`code @NotBlank`, dll) → N round-trip + partial fail berantakan. Endpoint price-only: satu request, per-item result.

## Testing

TDD:
- `DenomDomainServiceTest`: updatePrices happy path, not-found item, price ≤ 0 rejected, partial success (1 ok + 1 fail).
- `AdminCatalogControllerTest`: endpoint auth (manage_denoms), 200 dengan mixed result, 400 payload invalid.

## Notif denom belum ada harga (tambahan 2026-07-12)

Banner warning di atas tabel denoms: "⚠ N denom belum ada harga jual" (N = denom non-deleted dengan `price == null`). Tombol toggle → filter tabel tampilkan hanya yang belum ada harga. Client-side only — data sudah ada di Alpine state, zero backend.

## Out of scope

- Inline edit field selain `price` (basePrice, adminFee, active, dst.) — tambah nanti kalau perlu.
- Real-time multi-user sync.
- Halaman receipt terpisah.
