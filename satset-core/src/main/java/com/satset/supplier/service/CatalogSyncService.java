package com.satset.supplier.service;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.service.CatalogCodeUtil;
import com.satset.catalog.service.CategoryDomainService;
import com.satset.catalog.service.DenomDomainService;
import com.satset.catalog.service.ProductDomainService;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.logging.LogContext;
import com.satset.supplier.client.DigiflazzClient;
import com.satset.supplier.model.CompareStatus;
import com.satset.supplier.model.PriceCompareRow;
import com.satset.supplier.model.PriceListItem;
import com.satset.supplier.model.PriceListSnapshot;
import com.satset.supplier.model.SyncAction;
import com.satset.supplier.model.SyncPreviewItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
        List<PriceListItem> pl = digiflazz.fetchSnapshot().items();

        // --- kategori: tambah yang baru, set flag ---
        List<String> catAdds = previewCategories().stream()
                .filter(i -> i.action() == SyncAction.ADD).map(SyncPreviewItem::key).toList();
        SyncResult catRes = applyCategories(catAdds);
        Set<String> dfCatCodes = pl.stream()
                .map(i -> CatalogCodeUtil.toCode(i.category())).collect(Collectors.toSet());
        categoryService.reconcileSupplierFlags(dfCatCodes);

        int added = catRes.added(), updated = 0, failed = catRes.failed();

        for (Category c : categoryService.findAllForAdmin()) {
            if (c.isDeleted()) continue;
            UUID catId = c.getId();

            // --- produk: tambah baru, set flag ---
            List<String> prodAdds = previewProducts(catId).stream()
                    .filter(i -> i.action() == SyncAction.ADD).map(SyncPreviewItem::key).toList();
            SyncResult pr = applyProducts(catId, prodAdds);
            added += pr.added(); failed += pr.failed();
            Set<String> dfBrandCodes = pl.stream()
                    .filter(i -> CatalogCodeUtil.toCode(i.category()).equals(c.getCode()))
                    .map(i -> CatalogCodeUtil.toCode(i.brand())).collect(Collectors.toSet());
            productService.reconcileSupplierFlags(catId, dfBrandCodes);

            // --- denom per produk: tambah baru + update harga, set flag (HILANG tak dihapus) ---
            for (Products p : productService.findByCategoryForAdmin(catId)) {
                if (p.isDeleted()) continue;
                List<PriceCompareRow> rows = reconcileForProduct(p.getId());
                List<String> skus = rows.stream()
                        .filter(r -> r.status() != CompareStatus.SAMA && r.status() != CompareStatus.HILANG)
                        .map(PriceCompareRow::buyerSku).toList();
                SyncResult dr = applyDenoms(p.getId(), skus);
                added += dr.added(); updated += dr.updated(); failed += dr.failed();
                Set<String> dfSkuUpper = rows.stream()
                        .filter(r -> r.status() != CompareStatus.HILANG)
                        .map(r -> r.buyerSku().toUpperCase()).collect(Collectors.toSet());
                denomService.reconcileSupplierFlags(p.getId(), dfSkuUpper);
            }
        }
        return new SyncResult(added, updated, 0, 0, failed);
    }

    /**
     * Read-only: what a full {@link #syncAll()} would add/change, aggregated across the whole catalog.
     * ponytail: recompute preview/reconcile per level (fetchSnapshot di-cache 5 jam jadi murah);
     * kalau katalog membengkak & terasa lambat, cache pricelist di memori sekali per run.
     */
    public SyncAllPreview syncAllPreview() {
        List<String> newCategories = previewCategories().stream()
                .filter(i -> i.action() == SyncAction.ADD)
                .map(SyncPreviewItem::label).toList();

        List<String> newProducts = new ArrayList<>();
        List<String> newDenoms = new ArrayList<>();
        List<String> priceChanges = new ArrayList<>();
        for (Category c : categoryService.findAllForAdmin()) {
            if (c.isDeleted()) continue;
            previewProducts(c.getId()).stream()
                    .filter(i -> i.action() == SyncAction.ADD)
                    .forEach(i -> newProducts.add(c.getName() + " / " + i.label()));
            for (Products p : productService.findByCategoryForAdmin(c.getId())) {
                if (p.isDeleted()) continue;
                for (PriceCompareRow r : reconcileForProduct(p.getId())) {
                    if (r.status() == CompareStatus.BARU) newDenoms.add(p.getName() + " / " + r.productName());
                    else if (r.status() == CompareStatus.NAIK || r.status() == CompareStatus.TURUN)
                        priceChanges.add(p.getName() + " / " + r.buyerSku());
                }
            }
        }
        return new SyncAllPreview(newCategories, newProducts, newDenoms, priceChanges);
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

    /** Banding harga beli denom produk vs DF (buat kolom delta di halaman denom). */
    public List<PriceCompareRow> reconcileForProduct(UUID productId) {
        Products product = productService.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        String productCode = product.getCode();
        String catCode = categoryService.findById(product.getCategoryId())
                .map(Category::getCode).orElse(null);
        if (catCode == null) return List.of();

        // SKU DF utk brand produk ini DI KATEGORI INI (dedup by sku, harga terendah)
        Map<String, PriceListItem> uniq = new LinkedHashMap<>();
        for (PriceListItem it : digiflazz.fetchSnapshot().items()) {
            if (!CatalogCodeUtil.toCode(it.brand()).equals(productCode)) continue;
            if (!CatalogCodeUtil.toCode(it.category()).equals(catCode)) continue;
            uniq.merge(it.buyerSkuCode().toUpperCase(), it, (a, b) -> a.price() <= b.price() ? a : b);
        }
        // guard — brand tak match DF sama sekali -> jangan mass-deactivate; produk ini
        // dianggap bukan produk DF.
        if (uniq.isEmpty()) return List.of();

        Map<String, ProductDenoms> byCode = denomService.findActiveByProductId(productId).stream()
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
}
