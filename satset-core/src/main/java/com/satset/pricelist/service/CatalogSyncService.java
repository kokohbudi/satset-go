package com.satset.pricelist.service;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.service.CatalogCodeUtil;
import com.satset.catalog.service.category.CategoryDomainService;
import com.satset.catalog.service.denom.DenomDomainService;
import com.satset.catalog.service.product.ProductDomainService;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.logging.LogContext;
import com.satset.digiflazz.client.DigiflazzClient;
import com.satset.pricelist.model.CompareStatus;
import com.satset.pricelist.model.PriceCompareRow;
import com.satset.digiflazz.model.PriceListItem;
import com.satset.digiflazz.model.PriceListSnapshot;
import com.satset.pricelist.model.SyncAction;
import com.satset.pricelist.model.SyncPreviewItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Sync katalog per-level dgn Digiflazz: preview lalu apply selektif. Recompute dari DF fresh tiap aksi. */
@Slf4j
@Service
@LogContext("CatalogSyncService")
public class CatalogSyncService {

    private final DigiflazzClient digiflazz;
    private final CategoryDomainService categoryService;
    private final ProductDomainService productService;
    private final DenomDomainService denomService;

    public CatalogSyncService(DigiflazzClient digiflazz, CategoryDomainService categoryService,
                              ProductDomainService productService, DenomDomainService denomService) {
        this.digiflazz = digiflazz;
        this.categoryService = categoryService;
        this.productService = productService;
        this.denomService = denomService;
    }

    // ===== Categories =====
    public List<SyncPreviewItem> previewCategories() {
        List<Category> catalog = categoryService.findAllForAdmin();
        Set<String> catalogCodes = catalog.stream().filter(c -> !c.isDeleted())
                .map(Category::getCode).collect(Collectors.toSet());
        List<SyncPreviewItem> items = new ArrayList<>();
        Set<String> dfCodes = new HashSet<>();
        Set<String> seen = new HashSet<>();
        for (PriceListItem it : digiflazz.fetchSnapshot().items()) {
            String code = CatalogCodeUtil.toCode(it.category());
            dfCodes.add(code);
            if (seen.add(code) && !catalogCodes.contains(code)) {
                items.add(new SyncPreviewItem(SyncAction.ADD, it.category(), it.category(), null));
            }
        }
        for (Category c : catalog) {
            if (!c.isDeleted() && !dfCodes.contains(c.getCode())) {
                items.add(new SyncPreviewItem(SyncAction.DELETE, c.getId().toString(), c.getName(), "hilang dari DF"));
            }
        }
        return items;
    }

    public SyncResult applyCategories(List<String> keys) {
        Set<String> sel = new HashSet<>(keys);
        int added = 0, deleted = 0, skipped = 0, failed = 0;
        for (SyncPreviewItem it : previewCategories()) {
            if (!sel.contains(it.key())) { skipped++; continue; }
            try {
                switch (it.action()) {
                    case ADD -> { categoryService.findOrCreateByName(it.key()); added++; }
                    case DELETE -> { categoryService.softDelete(UUID.fromString(it.key())); deleted++; }
                    default -> skipped++;
                }
            } catch (Exception e) { log.error("applyCategories gagal utk {}", it.label(), e); failed++; }
        }
        return new SyncResult(added, 0, deleted, skipped, failed);
    }

    // ===== Products =====
    public List<SyncPreviewItem> previewProducts(UUID categoryId) {
        Category cat = categoryService.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        List<Products> catalog = productService.findByCategoryForAdmin(categoryId);
        List<SyncPreviewItem> items = new ArrayList<>();
        Set<String> dfBrandCodes = new HashSet<>();
        Set<String> seen = new HashSet<>();
        for (PriceListItem it : digiflazz.fetchSnapshot().items()) {
            if (!CatalogCodeUtil.toCode(it.category()).equals(cat.getCode())) continue;
            String code = CatalogCodeUtil.toCode(it.brand());
            dfBrandCodes.add(code);
            if (seen.add(code) && productService.findByCategoryAndCode(cat.getCode(), code).isEmpty()) {
                items.add(new SyncPreviewItem(SyncAction.ADD, it.brand(), it.brand(), null));
            }
        }
        for (Products p : catalog) {
            if (!p.isDeleted() && !dfBrandCodes.contains(p.getCode())) {
                items.add(new SyncPreviewItem(SyncAction.DELETE, p.getId().toString(), p.getName(), "hilang dari DF"));
            }
        }
        return items;
    }

    public SyncResult applyProducts(UUID categoryId, List<String> keys) {
        Set<String> sel = new HashSet<>(keys);
        int added = 0, deleted = 0, skipped = 0, failed = 0;
        for (SyncPreviewItem it : previewProducts(categoryId)) {
            if (!sel.contains(it.key())) { skipped++; continue; }
            try {
                switch (it.action()) {
                    case ADD -> { productService.findOrCreateByBrand(it.key(), categoryId); added++; }
                    case DELETE -> { productService.softDelete(UUID.fromString(it.key())); deleted++; }
                    default -> skipped++;
                }
            } catch (Exception e) { log.error("applyProducts gagal utk {}", it.label(), e); failed++; }
        }
        return new SyncResult(added, 0, deleted, skipped, failed);
    }

    // ===== Sync semua (kategori + produk + denom) =====

    /**
     * Sync penuh dengan supplier dalam satu aksi: tambah item baru + update harga (TANPA hapus).
     * Item yang hilang dari supplier TIDAK dihapus — cuma ditandai {@code inSupplier=false};
     * kalau muncul lagi, flag balik true.
     * ponytail: recompute preview/reconcile per level (fetchSnapshot di-cache 5 jam jadi murah);
     * kalau katalog membengkak & terasa lambat, cache pricelist di memori sekali per run.
     */
    public SyncResult syncAll() {
        // pakai snapshot cache (hormati rate-limit DF 5 jam); Harga Suplier refresh natural saat cache expire
        DfIndex df = indexDf(digiflazz.fetchSnapshot().items());

        // --- muat sekali (list admin di-cache tanpa TTL, evict tiap mutasi) ---
        Map<UUID, Category> catsById = new LinkedHashMap<>();
        for (Category c : categoryService.findAllForAdmin()) catsById.put(c.getId(), c);
        Map<UUID, List<Products>> prodsByCat = groupProductsByCategory(productService.findAllForAdmin());
        Map<UUID, List<ProductDenoms>> denomsByProduct = groupDenomsByProduct(denomService.findAllActiveForAdmin());

        int added = 0, failed = 0;

        // --- kategori: tambah yang baru ---
        for (SyncPreviewItem it : previewCategories()) {
            if (it.action() != SyncAction.ADD) continue;
            try { Category nc = categoryService.findOrCreateByName(it.key()); catsById.put(nc.getId(), nc); added++; }
            catch (Exception e) { log.error("syncAll tambah kategori {} gagal", it.label(), e); failed++; }
        }

        Map<UUID, Set<String>> prodFlagByCat = new HashMap<>();
        Map<UUID, Set<String>> denomFlagByProduct = new HashMap<>();

        for (Category c : catsById.values()) {
            if (c.isDeleted()) continue;
            String catCode = c.getCode();
            Set<String> dfBrands = df.brandsByCat().getOrDefault(catCode, Set.of());
            prodFlagByCat.put(c.getId(), dfBrands);
            List<Products> catalog = prodsByCat.computeIfAbsent(c.getId(), k -> new ArrayList<>());

            // --- produk: tambah brand DF yang belum ada (aktif) di katalog ---
            Set<String> activeCodes = catalog.stream().filter(p -> p.isActive() && !p.isDeleted())
                    .map(Products::getCode).collect(Collectors.toSet());
            for (String brandCode : dfBrands) {
                if (activeCodes.contains(brandCode)) continue;
                try {
                    Products np = productService.findOrCreateByBrand(df.brandName().get(catCode + "|" + brandCode), c.getId());
                    catalog.add(np); added++;
                } catch (Exception e) { log.error("syncAll tambah produk {} gagal", brandCode, e); failed++; }
            }

            // --- denom per produk: tambah denom BARU saja + kumpulkan flag (HILANG tak dihapus).
            //     Harga Beli (basePrice) denom existing TIDAK ditimpa — drift ditampilkan di tabel,
            //     admin apply Harga Suplier -> Harga Beli sendiri (per-row / bulk). ---
            for (Products p : catalog) {
                if (p.isDeleted()) continue;
                List<PriceListItem> dfItems = df.byCatBrand().getOrDefault(catCode + "|" + p.getCode(), List.of());
                if (dfItems.isEmpty()) continue;   // brand tak match DF -> jangan mass-deactivate
                List<PriceCompareRow> rows = compareRows(p, dfItems,
                        denomsByProduct.getOrDefault(p.getId(), List.of()));
                for (PriceCompareRow r : rows) {
                    if (r.status() != CompareStatus.BARU) continue;
                    try { denomService.createFromSupplier(p.getId(), r.buyerSku(), r.productName(), r.dfCost()); added++; }
                    catch (Exception e) { log.error("syncAll tambah denom {} gagal", r.buyerSku(), e); failed++; }
                }
                denomFlagByProduct.put(p.getId(), rows.stream()
                        .filter(r -> r.status() != CompareStatus.HILANG)
                        .map(r -> r.buyerSku().toUpperCase()).collect(Collectors.toSet()));
            }
        }

        // --- set flag inSupplier sekali per level (batch, bukan query per item) ---
        categoryService.reconcileSupplierFlags(df.catCodes());
        productService.reconcileSupplierFlags(prodFlagByCat);
        denomService.reconcileSupplierFlags(denomFlagByProduct);

        return new SyncResult(added, 0, 0, 0, failed);
    }

    /**
     * Read-only: what a full {@link #syncAll()} would add/change, aggregated across the whole catalog.
     * ponytail: recompute preview/reconcile per level (fetchSnapshot di-cache 5 jam jadi murah);
     * kalau katalog membengkak & terasa lambat, cache pricelist di memori sekali per run.
     */
    public SyncAllPreview syncAllPreview() {
        // produk baru = brand DF yang belum ada (aktif) di katalog — level brand, bukan per compareRow
        DfIndex df = indexDf(digiflazz.fetchSnapshot().items());
        Map<UUID, List<Products>> prodsByCat = groupProductsByCategory(productService.findAllForAdmin());

        List<String> newCategories = previewCategories().stream()
                .filter(i -> i.action() == SyncAction.ADD)
                .map(SyncPreviewItem::label).toList();

        List<String> newProducts = new ArrayList<>();
        for (Category c : categoryService.findAllForAdmin()) {
            if (c.isDeleted()) continue;
            Set<String> activeCodes = prodsByCat.getOrDefault(c.getId(), List.of()).stream()
                    .filter(p -> p.isActive() && !p.isDeleted()).map(Products::getCode).collect(Collectors.toSet());
            for (String brandCode : df.brandsByCat().getOrDefault(c.getCode(), Set.of()))
                if (!activeCodes.contains(brandCode))
                    newProducts.add(c.getName() + " / " + df.brandName().get(c.getCode() + "|" + brandCode));
        }

        // denom baru / beda harga / hilang — walk bersama dgn deactivate (reuse df+prodsByCat, no double indexDf)
        List<String> newDenoms = new ArrayList<>(), priceChanges = new ArrayList<>(), removed = new ArrayList<>();
        eachSupplierCompare(df, prodsByCat, (p, r) -> {
            switch (r.status()) {
                case BARU -> newDenoms.add(p.getName() + " / " + r.productName());
                case NAIK, TURUN -> priceChanges.add(p.getName() + " / " + r.buyerSku());
                case HILANG -> removed.add(p.getName() + " / " + r.buyerSku());
                default -> { }
            }
        });
        return new SyncAllPreview(newCategories, newProducts, newDenoms, priceChanges, removed);
    }

    /**
     * Nonaktifkan (active=false) semua denom yang HILANG dari suplier — SKU-nya tak ada lagi di DF
     * untuk brand+kategori produk DF-nya. Recompute di server (tak percaya id stale dari client).
     * Bukan hapus (deleted tetap false) — reversible, muncul lagi kalau suplier balikin SKU-nya.
     * @return jumlah denom yang dinonaktifkan.
     */
    public int deactivateMissingFromSupplier() {
        List<UUID> missing = new ArrayList<>();
        eachSupplierCompare((p, r) -> {
            if (r.status() == CompareStatus.HILANG && r.denomId() != null) missing.add(r.denomId());
        });
        return denomService.deactivate(missing);
    }

    // ===== Denoms (preview = reconcileForProduct; UI map status->action) =====
    public SyncResult applyDenoms(UUID productId, List<String> selectedSkus) {
        Set<String> sel = new HashSet<>(selectedSkus);
        int added = 0, updated = 0, deleted = 0, skipped = 0, failed = 0;
        for (PriceCompareRow r : reconcileForProduct(productId)) {
            if (r.status() == CompareStatus.SAMA || !sel.contains(r.buyerSku())) { skipped++; continue; }
            try {
                switch (r.status()) {
                    case BARU -> { denomService.createFromSupplier(productId, r.buyerSku(), r.productName(), r.dfCost()); added++; }
                    case NAIK, TURUN -> { denomService.updateCostById(r.denomId(), r.dfCost()); updated++; }
                    case HILANG -> { denomService.softDelete(r.denomId()); deleted++; }
                    case SAMA -> skipped++;
                }
            } catch (Exception e) { log.error("applyDenoms gagal utk SKU {}", r.buyerSku(), e); failed++; }
        }
        return new SyncResult(added, updated, deleted, skipped, failed);
    }

    /** Banding harga beli denom produk vs DF (buat kolom delta di halaman denom). Single-item: muat per id. */
    public List<PriceCompareRow> reconcileForProduct(UUID productId) {
        Products product = productService.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        String catCode = categoryService.findById(product.getCategoryId())
                .map(Category::getCode).orElse(null);
        if (catCode == null) return List.of();
        List<PriceListItem> dfItems = new ArrayList<>();
        for (PriceListItem it : digiflazz.fetchSnapshot().items()) {
            if (CatalogCodeUtil.toCode(it.brand()).equals(product.getCode())
                    && CatalogCodeUtil.toCode(it.category()).equals(catCode)) dfItems.add(it);
        }
        // guard — brand tak match DF sama sekali -> jangan mass-deactivate; produk ini bukan produk DF.
        if (dfItems.isEmpty()) return List.of();
        return compareRows(product, dfItems, denomService.findActiveByProductId(productId));
    }

    /**
     * Compare murni (tanpa DB): DF item utk brand+kategori produk ini (sudah difilter) vs denom aktif.
     * Dipakai single-item {@link #reconcileForProduct} maupun batch {@link #syncAll}/{@link #syncAllPreview}.
     */
    private List<PriceCompareRow> compareRows(Products product, List<PriceListItem> dfForBrandCat,
                                              List<ProductDenoms> activeDenoms) {
        // dedup by sku, harga terendah menang
        Map<String, PriceListItem> uniq = new LinkedHashMap<>();
        for (PriceListItem it : dfForBrandCat) {
            uniq.merge(it.buyerSkuCode().toUpperCase(), it, (a, b) -> a.price() <= b.price() ? a : b);
        }
        if (uniq.isEmpty()) return List.of();
        Map<String, ProductDenoms> byCode = activeDenoms.stream()
                .collect(Collectors.toMap(d -> d.getCode().toUpperCase(), Function.identity(), (a, b) -> a));
        List<PriceCompareRow> rows = new ArrayList<>();
        Set<String> matched = new HashSet<>();
        for (PriceListItem it : uniq.values()) {
            BigDecimal dfCost = BigDecimal.valueOf(it.price());
            ProductDenoms denom = byCode.get(it.buyerSkuCode().toUpperCase());
            if (denom == null) {
                rows.add(new PriceCompareRow(it.buyerSkuCode(), it.productName(), it.brand(), it.category(),
                        it.sellerName(), null, dfCost, null, null, CompareStatus.BARU));
                continue;
            }
            matched.add(denom.getCode().toUpperCase());
            BigDecimal dbCost = denom.getBasePrice();
            CompareStatus status;
            BigDecimal delta = null;
            if (dbCost == null) { status = CompareStatus.NAIK; }
            else {
                int cmp = dbCost.compareTo(dfCost);
                status = cmp == 0 ? CompareStatus.SAMA : cmp < 0 ? CompareStatus.NAIK : CompareStatus.TURUN;
                delta = dfCost.subtract(dbCost);
            }
            rows.add(new PriceCompareRow(it.buyerSkuCode(), it.productName(), it.brand(), it.category(),
                    it.sellerName(), dbCost, dfCost, delta, denom.getId(), status));
        }
        byCode.forEach((code, denom) -> {
            if (!matched.contains(code)) {
                rows.add(new PriceCompareRow(denom.getCode(), denom.getName(), product.getName(), null,
                        null, denom.getBasePrice(), null, null, denom.getId(), CompareStatus.HILANG));
            }
        });
        return rows;
    }

    // ===== Index DF snapshot sekali (in-memory), biar sync tak walk snapshot per item =====

    /**
     * @param catCodes    set code kategori DF (buat flag kategori)
     * @param brandsByCat catCode -> set code brand DF (buat deteksi produk baru + flag)
     * @param byCatBrand  "catCode|brandCode" -> DF item (buat compare denom)
     * @param brandName   "catCode|brandCode" -> nama brand DF asli (first-seen, buat label produk baru)
     */
    private record DfIndex(Set<String> catCodes, Map<String, Set<String>> brandsByCat,
                           Map<String, List<PriceListItem>> byCatBrand, Map<String, String> brandName) {}

    private static DfIndex indexDf(List<PriceListItem> items) {
        Set<String> catCodes = new HashSet<>();
        Map<String, Set<String>> brandsByCat = new HashMap<>();
        Map<String, List<PriceListItem>> byCatBrand = new HashMap<>();
        Map<String, String> brandName = new HashMap<>();
        for (PriceListItem it : items) {
            String cc = CatalogCodeUtil.toCode(it.category());
            String bc = CatalogCodeUtil.toCode(it.brand());
            String key = cc + "|" + bc;
            catCodes.add(cc);
            brandsByCat.computeIfAbsent(cc, k -> new HashSet<>()).add(bc);
            byCatBrand.computeIfAbsent(key, k -> new ArrayList<>()).add(it);
            brandName.putIfAbsent(key, it.brand());
        }
        return new DfIndex(catCodes, brandsByCat, byCatBrand, brandName);
    }

    private static Map<UUID, List<Products>> groupProductsByCategory(List<Products> products) {
        Map<UUID, List<Products>> byCat = new LinkedHashMap<>();
        for (Products p : products) byCat.computeIfAbsent(p.getCategoryId(), k -> new ArrayList<>()).add(p);
        return byCat;
    }

    private static Map<UUID, List<ProductDenoms>> groupDenomsByProduct(List<ProductDenoms> denoms) {
        // read-only downstream (tak pernah di-mutate) -> groupingBy aman
        return denoms.stream().collect(Collectors.groupingBy(ProductDenoms::getProductId));
    }

    /**
     * Walk katalog vs suplier sekali: tiap produk-DF, hasil {@link #compareRows} diteruskan ke {@code visit}.
     * Muat 3 list admin (cached) + snapshot DF (cached) = O(1) query. Dipakai bareng oleh
     * {@link #syncAllPreview()} (denom baru/beda/hilang) & {@link #deactivateMissingFromSupplier()}.
     */
    /** Bangun context sendiri lalu delegasi. Dipakai {@link #deactivateMissingFromSupplier()} yang tak pegang data. */
    private void eachSupplierCompare(BiConsumer<Products, PriceCompareRow> visit) {
        eachSupplierCompare(indexDf(digiflazz.fetchSnapshot().items()),
                groupProductsByCategory(productService.findAllForAdmin()), visit);
    }

    /** Pakai {@code df}+{@code prodsByCat} yang sudah dibangun caller (hindari double indexDf). Denom di-load di sini. */
    private void eachSupplierCompare(DfIndex df, Map<UUID, List<Products>> prodsByCat,
                                     BiConsumer<Products, PriceCompareRow> visit) {
        Map<UUID, List<ProductDenoms>> denomsByProduct = groupDenomsByProduct(denomService.findAllActiveForAdmin());
        for (Category c : categoryService.findAllForAdmin()) {
            if (c.isDeleted()) continue;
            for (Products p : prodsByCat.getOrDefault(c.getId(), List.of())) {
                if (p.isDeleted()) continue;
                List<PriceListItem> dfItems = df.byCatBrand().getOrDefault(c.getCode() + "|" + p.getCode(), List.of());
                if (dfItems.isEmpty()) continue;   // produk bukan DF -> jangan sentuh (guard sama kayak reconcile)
                for (PriceCompareRow r : compareRows(p, dfItems, denomsByProduct.getOrDefault(p.getId(), List.of())))
                    visit.accept(p, r);
            }
        }
    }

    // ===== Supplier prices (global SKU->cheapest cost map, for admin Harga Suplier column) =====

    /** Global SKU(upper) -> cheapest DF cost, from the cached snapshot (no forced fetch). */
    public SupplierPriceView supplierPrices() {
        PriceListSnapshot snap = digiflazz.fetchSnapshot();
        Map<String, Long> prices = new LinkedHashMap<>();
        for (PriceListItem it : snap.items()) {
            prices.merge(it.buyerSkuCode().toUpperCase(), it.price(), Math::min);
        }
        return new SupplierPriceView(snap.fetchedAt(), prices);
    }

    /**
     * Terapkan Harga Suplier (cost DF) -> Harga Beli (basePrice) untuk denom terpilih.
     * Cost diambil dari snapshot cache (by denom code); denom tanpa match DF dilewati.
     * @return jumlah denom yang benar-benar di-update.
     */
    public int applySupplierCostBulk(List<UUID> denomIds) {
        // batch di domain service: 1 load + write di-batch (bukan findById+updateCostById per item)
        return denomService.applySupplierCost(denomIds, supplierPrices().prices());
    }
}
