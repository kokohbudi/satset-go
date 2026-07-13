package com.satset.supplier.web;

import com.satset.shared.constant.SatsetConstants;
import com.satset.supplier.model.PriceCompareRow;
import com.satset.supplier.model.SyncPreviewItem;
import com.satset.supplier.service.CatalogSyncService;
import com.satset.supplier.service.SupplierPriceView;
import com.satset.supplier.service.SyncAllPreview;
import com.satset.supplier.service.SyncResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Endpoint preview + apply sync katalog per-level dgn Digiflazz. */
@RestController
@RequestMapping("/api/admin/catalog")
public class CatalogSyncController {

    private final CatalogSyncService sync;
    public CatalogSyncController(CatalogSyncService sync) { this.sync = sync; }

    // Sync semua (kategori + produk + denom) sekaligus
    @PostMapping("/sync/all")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_CATEGORIES + "')")
    public SyncResult syncAll() { return sync.syncAll(); }

    // Read-only aggregate summary of what syncAll() would add/change
    @GetMapping("/sync/all/preview")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_CATEGORIES + "')")
    public SyncAllPreview syncAllPreview() { return sync.syncAllPreview(); }

    // Categories
    @GetMapping("/sync/categories/preview")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_CATEGORIES + "')")
    public List<SyncPreviewItem> previewCategories() { return sync.previewCategories(); }

    @PostMapping("/sync/categories")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_CATEGORIES + "')")
    public SyncResult applyCategories(@RequestBody List<String> keys) { return sync.applyCategories(keys); }

    // Products
    @GetMapping("/categories/{categoryId}/sync/products/preview")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_PRODUCTS + "')")
    public List<SyncPreviewItem> previewProducts(@PathVariable UUID categoryId) { return sync.previewProducts(categoryId); }

    @PostMapping("/categories/{categoryId}/sync/products")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_PRODUCTS + "')")
    public SyncResult applyProducts(@PathVariable UUID categoryId, @RequestBody List<String> keys) {
        return sync.applyProducts(categoryId, keys);
    }

    // Denoms (preview = compare)
    @GetMapping("/products/{productId}/pricelist-compare")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public List<PriceCompareRow> compare(@PathVariable UUID productId) { return sync.reconcileForProduct(productId); }

    @PostMapping("/products/{productId}/sync/denoms")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public SyncResult applyDenoms(@PathVariable UUID productId, @RequestBody List<String> selectedSkus) {
        return sync.applyDenoms(productId, selectedSkus);
    }

    // Global SKU->cheapest DF cost map (admin Harga Suplier column)
    @GetMapping("/supplier-prices")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_CATALOG + "')")
    public SupplierPriceView supplierPrices() { return sync.supplierPrices(); }

    // Apply Harga Suplier -> Harga Beli (basePrice), per denom
    @PostMapping("/denoms/{id}/apply-supplier-cost")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public Map<String, Integer> applySupplierCost(@PathVariable UUID id) {
        return Map.of("applied", sync.applySupplierCostBulk(List.of(id)));
    }

    // Apply Harga Suplier -> Harga Beli, bulk (id denom terpilih)
    @PostMapping("/denoms/apply-supplier-cost")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public Map<String, Integer> applySupplierCostBulk(@RequestBody List<UUID> denomIds) {
        return Map.of("applied", sync.applySupplierCostBulk(denomIds));
    }

    // Nonaktifkan semua denom yang HILANG dari suplier (recompute di server, active=false)
    @PostMapping("/denoms/deactivate-missing")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public Map<String, Integer> deactivateMissing() {
        return Map.of("deactivated", sync.deactivateMissingFromSupplier());
    }
}
