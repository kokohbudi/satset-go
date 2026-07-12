package com.satset.supplier.web;

import com.satset.shared.constant.OmniConstants;
import com.satset.supplier.model.PriceCompareRow;
import com.satset.supplier.model.SyncPreviewItem;
import com.satset.supplier.service.CatalogSyncService;
import com.satset.supplier.service.SupplierPriceView;
import com.satset.supplier.service.SyncResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Endpoint preview + apply sync katalog per-level dgn Digiflazz. */
@RestController
@RequestMapping("/api/admin/catalog")
public class CatalogSyncController {

    private final CatalogSyncService sync;
    public CatalogSyncController(CatalogSyncService sync) { this.sync = sync; }

    // Sync semua (kategori + produk + denom) sekaligus
    @PostMapping("/sync/all")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public SyncResult syncAll() { return sync.syncAll(); }

    // Categories
    @GetMapping("/sync/categories/preview")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public List<SyncPreviewItem> previewCategories() { return sync.previewCategories(); }

    @PostMapping("/sync/categories")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public SyncResult applyCategories(@RequestBody List<String> keys) { return sync.applyCategories(keys); }

    // Products
    @GetMapping("/categories/{categoryId}/sync/products/preview")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public List<SyncPreviewItem> previewProducts(@PathVariable UUID categoryId) { return sync.previewProducts(categoryId); }

    @PostMapping("/categories/{categoryId}/sync/products")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public SyncResult applyProducts(@PathVariable UUID categoryId, @RequestBody List<String> keys) {
        return sync.applyProducts(categoryId, keys);
    }

    // Denoms (preview = compare)
    @GetMapping("/products/{productId}/pricelist-compare")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public List<PriceCompareRow> compare(@PathVariable UUID productId) { return sync.reconcileForProduct(productId); }

    @PostMapping("/products/{productId}/sync/denoms")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public SyncResult applyDenoms(@PathVariable UUID productId, @RequestBody List<String> selectedSkus) {
        return sync.applyDenoms(productId, selectedSkus);
    }

    // Global SKU->cheapest DF cost map (admin Harga Suplier column)
    @GetMapping("/supplier-prices")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public SupplierPriceView supplierPrices() { return sync.supplierPrices(); }
}
