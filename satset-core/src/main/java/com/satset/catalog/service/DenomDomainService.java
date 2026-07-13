package com.satset.catalog.service;

import com.satset.catalog.repository.CategoryRepository;
import com.satset.catalog.repository.DenomRepository;
import com.satset.catalog.repository.ProductRepository;
import com.satset.catalog.model.Category;
import com.satset.catalog.model.DenomType;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.dto.CreateDenomRequest;
import com.satset.catalog.dto.UpdateDenomRequest;
import com.satset.catalog.dto.BulkNameUpdateRequest;
import com.satset.catalog.dto.BulkPriceUpdateRequest;
import com.satset.catalog.dto.PriceUpdateResult;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DenomDomainService {

    private final DenomRepository denomRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public DenomDomainService(DenomRepository denomRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        this.denomRepository = denomRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // === Browse (read-only) ===

    public List<ProductDenoms> findByProduct(String categoryCode, String productCode) {
        Optional<Category> category = categoryRepository.findByCode(categoryCode);
        if (category.isEmpty()) return List.of();
        Optional<Products> product =
                productRepository.findByCategoryIdAndCode(category.get().getId(), productCode)
                        .filter(p -> p.isActive() && !p.isDeleted());
        if (product.isEmpty()) return List.of();
        return denomRepository
                .findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(product.get().getId());
    }

    public Optional<ProductDenoms> getDenomWithMeta(String code) {
        return denomRepository.findByCode(code)
                .filter(d -> d.isActive() && !d.isDeleted());
    }

    /** All denoms incl. deleted, for the admin aggregate view (client greys deleted). */
    public List<ProductDenoms> findAllForAdmin() {
        return denomRepository.findAllByOrderBySortOrder();
    }

    // === Manage (admin CRUD) ===

    public List<ProductDenoms> findByProductForAdmin(UUID productId) {
        return denomRepository.findByProductIdOrderBySortOrder(productId);
    }

    public Optional<ProductDenoms> findById(UUID id) {
        return denomRepository.findById(id);
    }

    @Transactional
    @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    public ProductDenoms create(UUID productId, CreateDenomRequest req) throws BusinessException {
        Products product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        String code = (req.code() == null || req.code().isBlank())
            ? generateSku(product, req.nominal())
            : req.code().toUpperCase().trim();
        if (denomRepository.findByCode(code).isPresent()) {
            throw new BusinessException("DUPLICATE_CODE", "Denom code already exists: " + code);
        }
        ProductDenoms denom = new ProductDenoms();
        denom.setProductId(product.getId());
        denom.setCode(code);
        denom.setName(req.name());
        denom.setDenomType(req.denomType());
        denom.setNominal(req.nominal());
        denom.setPrice(req.price());
        denom.setBasePrice(req.basePrice());
        denom.setAdminFee(req.adminFee());
        denom.setValidityDays(req.validityDays());
        denom.setQuotaMb(req.quotaMb());
        denom.setMinAmount(req.minAmount());
        denom.setMaxAmount(req.maxAmount());
        denom.setRequiresInquiry(req.requiresInquiry());
        denom.setStockAvailable(req.stockAvailable());
        denom.setActive(req.active());
        denom.setSortOrder(req.sortOrder());
        denom.setDeleted(false);
        return denomRepository.save(denom);
    }

    /**
     * SKU otomatis: PRODUCT+NOMINAL tanpa dash (mis. TELKOMSEL10000). Open amount (nominal null)
     * pakai nomor urut (TELKOMSEL1). Bentrok → tambah angka di belakang.
     * ponytail: pada bentrok, suffix numerik bisa ambigu (TELKOMSEL10000 + 2 = TELKOMSEL100002);
     * kasus langka, tambahkan separator kalau ini pernah jadi masalah.
     */
    private String generateSku(Products product, BigDecimal nominal) {
        String base = product.getCode();
        String candidate = nominal != null ? base + nominal.toBigInteger() : base + "1";
        int n = 2;
        while (denomRepository.findByCode(candidate).isPresent()) {
            candidate = (nominal != null ? base + nominal.toBigInteger() : base) + n++;
        }
        return candidate;
    }

    @Transactional
    @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    public ProductDenoms update(UUID id, UpdateDenomRequest req) throws BusinessException {
        ProductDenoms denom = denomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Denom", id));
        if (denomRepository.existsByCodeAndIdNot(req.code().toUpperCase().trim(), id)) {
            throw new BusinessException("DUPLICATE_CODE", "Denom code already exists: " + req.code());
        }
        denom.setCode(req.code().toUpperCase().trim());
        denom.setName(req.name());
        denom.setDenomType(req.denomType());
        denom.setNominal(req.nominal());
        denom.setPrice(req.price());
        denom.setBasePrice(req.basePrice());
        denom.setAdminFee(req.adminFee());
        denom.setValidityDays(req.validityDays());
        denom.setQuotaMb(req.quotaMb());
        denom.setMinAmount(req.minAmount());
        denom.setMaxAmount(req.maxAmount());
        denom.setRequiresInquiry(req.requiresInquiry());
        denom.setStockAvailable(req.stockAvailable());
        denom.setActive(req.active());
        denom.setSortOrder(req.sortOrder());
        if (req.productId() != null && !req.productId().equals(denom.getProductId())) {
            productRepository.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", req.productId()));
            denom.setProductId(req.productId());
        }
        return denomRepository.save(denom);
    }

    @Transactional
    @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    public void softDelete(UUID id) {
        ProductDenoms denom = denomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Denom", id));
        denom.setDeleted(true);
        denom.setActive(false);
        denomRepository.save(denom);
    }

    // === Bulk price update (inline edit harga jual) ===

    /**
     * Update harga jual banyak denom sekaligus. Validation error (tidak ditemukan,
     * harga <= 0, sudah dihapus) tetap per-item — dicek SEBELUM save, item lain jalan terus.
     * Persistensi satu transaksi: semua save di-commit bareng, all-or-nothing.
     * ponytail: optimistic-lock conflict (edit bersamaan) menggagalkan seluruh batch —
     * UI pertahankan dirty state, user coba lagi.
     */
    @Transactional
    @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    public List<PriceUpdateResult> updatePrices(List<BulkPriceUpdateRequest> items) {
        Map<UUID, ProductDenoms> byId = denomsById(items.stream().map(BulkPriceUpdateRequest::id).toList());
        List<PriceUpdateResult> results = new ArrayList<>(items.size());
        for (BulkPriceUpdateRequest item : items) {
            results.add(updateSinglePrice(item, byId));
        }
        return results;
    }

    /** Muat denom target sekali (1 query IN (...)) daripada findById per item. Null id dibuang. */
    private Map<UUID, ProductDenoms> denomsById(List<UUID> ids) {
        List<UUID> present = ids.stream().filter(Objects::nonNull).toList();
        if (present.isEmpty()) return Map.of();
        return denomRepository.findAllById(present).stream()
                .collect(Collectors.toMap(ProductDenoms::getId, Function.identity()));
    }

    /**
     * Update nama banyak denom sekaligus. Validation error per-item (dicek sebelum save).
     * Satu transaksi, all-or-nothing. Reuse {@link PriceUpdateResult} sebagai amplop hasil.
     */
    @Transactional
    @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    public List<PriceUpdateResult> updateNames(List<BulkNameUpdateRequest> items) {
        Map<UUID, ProductDenoms> byId = denomsById(items.stream().map(BulkNameUpdateRequest::id).toList());
        List<PriceUpdateResult> results = new ArrayList<>(items.size());
        for (BulkNameUpdateRequest item : items) {
            results.add(updateSingleName(item, byId));
        }
        return results;
    }

    private PriceUpdateResult updateSingleName(BulkNameUpdateRequest item, Map<UUID, ProductDenoms> byId) {
        if (item.id() == null) {
            return PriceUpdateResult.fail(null, null, "Denom tidak ditemukan");
        }
        ProductDenoms denom = byId.get(item.id());
        if (denom == null) {
            return PriceUpdateResult.fail(item.id(), null, "Denom tidak ditemukan");
        }
        String name = item.name() == null ? "" : item.name().trim();
        if (name.isEmpty()) {
            return PriceUpdateResult.fail(item.id(), denom.getCode(), "Nama kosong");
        }
        if (name.length() > 150) {
            return PriceUpdateResult.fail(item.id(), denom.getCode(), "Nama terlalu panjang");
        }
        if (denom.isDeleted()) {
            return PriceUpdateResult.fail(item.id(), denom.getCode(), "Denom sudah dihapus");
        }
        denom.setName(name);
        denomRepository.save(denom);
        return PriceUpdateResult.ok(item.id(), denom.getCode());
    }

    private PriceUpdateResult updateSinglePrice(BulkPriceUpdateRequest item, Map<UUID, ProductDenoms> byId) {
        if (item.id() == null) {
            return PriceUpdateResult.fail(null, null, "Denom tidak ditemukan");
        }
        ProductDenoms denom = byId.get(item.id());
        if (denom == null) {
            return PriceUpdateResult.fail(item.id(), null, "Denom tidak ditemukan");
        }
        if (item.price() == null || item.price().signum() <= 0) {
            return PriceUpdateResult.fail(item.id(), denom.getCode(), "Harga harus > 0");
        }
        if (denom.isDeleted()) {
            return PriceUpdateResult.fail(item.id(), denom.getCode(), "Denom sudah dihapus");
        }
        denom.setPrice(item.price());
        denomRepository.save(denom);
        return PriceUpdateResult.ok(item.id(), denom.getCode());
    }

    // === Supplier sync (Digiflazz) ===

    public List<ProductDenoms> findActiveByProductId(UUID productId) {
        return denomRepository.findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(productId);
    }

    /**
     * Semua denom aktif lintas produk, buat sync batch (group by productId di memori).
     * Cached tanpa TTL — evict tiap mutasi denom (lihat {@code @CacheEvict} di method write).
     */
    @Cacheable(value = "adminActiveDenoms", cacheManager = "catalogCacheManager")
    public List<ProductDenoms> findAllActiveForAdmin() {
        return denomRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder();
    }

    @Transactional
    @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    public ProductDenoms createFromSupplier(UUID productId, String sku, String name, BigDecimal cost) {
        // revive kalau code (soft-deleted) sudah ada — hindari UNIQUE violation + biar gak muncul terus di sync
        ProductDenoms d = denomRepository.findByCode(sku).orElseGet(ProductDenoms::new);
        d.setProductId(productId);
        d.setCode(sku);                 // apa adanya — JANGAN uppercase
        d.setName(name);
        d.setDenomType(DenomType.FIXED_DENOM);
        d.setBasePrice(cost);
        d.setActive(true);
        d.setDeleted(false);
        return denomRepository.save(d);
    }

    @Transactional
    @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    public void updateCostById(UUID denomId, BigDecimal cost) {
        ProductDenoms d = denomRepository.findById(denomId)
                .orElseThrow(() -> new ResourceNotFoundException("Denom", denomId));
        d.setBasePrice(cost);
        denomRepository.save(d);
    }

    /**
     * Batch: apply Harga Suplier -> Harga Beli (basePrice) utk denom terpilih dalam 1 transaksi.
     * Cost by code (uppercase); denom tanpa match dilewati. 1 load ({@code findAllById}), UPDATE
     * ditunda + di-batch Hibernate saat commit (entity managed) — bukan trip per item.
     * All-or-nothing: 1 optimistic-lock conflict -> rollback semua (user tinggal ulangi).
     * @return jumlah denom yang benar-benar di-update.
     */
    @Transactional
    @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    public int applySupplierCost(List<UUID> denomIds, Map<String, Long> costByCodeUpper) {
        int applied = 0;
        for (ProductDenoms d : denomRepository.findAllById(denomIds)) {
            Long cost = costByCodeUpper.get(d.getCode().toUpperCase());
            if (cost == null) continue;
            d.setBasePrice(BigDecimal.valueOf(cost));   // managed -> dirty-flush di-batch saat commit
            applied++;
        }
        return applied;
    }

    /**
     * Batch: set flag inSupplier semua denom aktif dalam 1 load (bukan query per produk).
     * {@code supplierSkusUpperByProduct}: productId -> set SKU (uppercase) yang ada di supplier.
     * Denom yang produknya tak ada di map dianggap tak punya SKU supplier (flag false).
     */
    @Transactional
    @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    public int reconcileSupplierFlags(Map<UUID, Set<String>> supplierSkusUpperByProduct) {
        int changed = 0;
        for (ProductDenoms d : denomRepository.findAllByOrderBySortOrder()) {
            if (d.isDeleted()) continue;
            boolean present = supplierSkusUpperByProduct
                    .getOrDefault(d.getProductId(), Set.of()).contains(d.getCode().toUpperCase());
            if (d.isInSupplier() != present) { d.setInSupplier(present); denomRepository.save(d); changed++; }
        }
        return changed;
    }
}