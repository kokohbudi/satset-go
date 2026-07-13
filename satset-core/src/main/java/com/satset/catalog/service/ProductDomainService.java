package com.satset.catalog.service;

import com.satset.catalog.repository.CategoryRepository;
import com.satset.catalog.repository.DenomRepository;
import com.satset.catalog.repository.ProductRepository;
import com.satset.catalog.model.Category;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.dto.BulkNameUpdateRequest;
import com.satset.catalog.dto.CreateProductRequest;
import com.satset.catalog.dto.PriceUpdateResult;
import com.satset.catalog.dto.UpdateProductRequest;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class ProductDomainService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final DenomRepository denomRepository;

    public ProductDomainService(ProductRepository productRepository,
                                CategoryRepository categoryRepository,
                                DenomRepository denomRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.denomRepository = denomRepository;
    }

    // === Browse (read-only, cached) ===

    public List<Products> findByCategory(String categoryCode) {
        Optional<Category> category = categoryRepository.findByCode(categoryCode);
        if (category.isEmpty()) {
            return List.of();
        }
        return productRepository.findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(category.get().getId());
    }

    @Cacheable(value = "products", cacheManager = "standardCacheManager")
    public List<Products> findActiveProducts() {
        return productRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder();
    }

    public Optional<Products> findByCategoryAndCode(String categoryCode, String code) {
        Optional<Category> category = categoryRepository.findByCode(categoryCode);
        if (category.isEmpty()) return Optional.empty();
        return productRepository.findByCategoryIdAndCode(category.get().getId(), code)
                .filter(p -> p.isActive() && !p.isDeleted());
    }

    // === Manage (admin CRUD) ===

    public List<Products> findByCategoryForAdmin(UUID categoryId) {
        return productRepository.findByCategoryIdOrderBySortOrder(categoryId);
    }

    /** Semua produk hidup, buat sync batch + tabel admin. Cached tanpa TTL — evict tiap mutasi produk. */
    @Cacheable(value = "adminProducts", cacheManager = "catalogCacheManager")
    public List<Products> findAllForAdmin() {
        return productRepository.findByDeletedFalseOrderBySortOrder();
    }

    public Optional<Products> findById(UUID id) {
        return productRepository.findById(id);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminProducts", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public Products create(CreateProductRequest req) throws BusinessException {
        Category category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        if (productRepository.findByCategoryIdAndCode(category.getId(), req.code().toUpperCase().trim()).isPresent()) {
            throw new BusinessException("DUPLICATE_CODE", "Product code already exists: " + req.code());
        }
        Products product = new Products();
        product.setCategoryId(category.getId());
        product.setCode(req.code().toUpperCase().trim());
        product.setName(req.name());
        product.setProviderName(req.providerName());
        product.setDescription(req.description());
        product.setIconUrl(req.iconUrl());
        product.setActive(req.active());
        product.setSortOrder(req.sortOrder());
        product.setDeleted(false);
        return productRepository.save(product);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminProducts", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public Products update(UUID id, UpdateProductRequest req) throws BusinessException {
        Products product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        Category category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        if (productRepository.existsByCategoryIdAndCodeAndIdNot(category.getId(), req.code().toUpperCase().trim(), id)) {
            throw new BusinessException("DUPLICATE_CODE", "Product code already exists: " + req.code());
        }
        product.setCategoryId(category.getId());
        product.setCode(req.code().toUpperCase().trim());
        product.setName(req.name());
        product.setProviderName(req.providerName());
        product.setDescription(req.description());
        product.setIconUrl(req.iconUrl());
        product.setActive(req.active());
        product.setSortOrder(req.sortOrder());
        return productRepository.save(product);
    }

    // === Bulk name update (inline edit nama produk) ===

    /**
     * Update nama banyak produk sekaligus. Validation error (tidak ditemukan, nama
     * kosong/terlalu panjang, sudah dihapus) per-item — dicek SEBELUM save, item lain jalan.
     * Persistensi satu transaksi: reuse {@link PriceUpdateResult} sebagai amplop hasil generik.
     * ponytail: optimistic-lock conflict menggagalkan seluruh batch — UI simpan dirty, coba lagi.
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminProducts", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public List<PriceUpdateResult> updateNames(List<BulkNameUpdateRequest> items) {
        List<UUID> ids = items.stream().map(BulkNameUpdateRequest::id).filter(Objects::nonNull).toList();
        Map<UUID, Products> byId = ids.isEmpty() ? Map.of()
                : productRepository.findAllById(ids).stream()
                        .collect(Collectors.toMap(Products::getId, Function.identity()));
        List<PriceUpdateResult> results = new ArrayList<>(items.size());
        for (BulkNameUpdateRequest item : items) {
            results.add(updateSingleName(item, byId));
        }
        return results;
    }

    private PriceUpdateResult updateSingleName(BulkNameUpdateRequest item, Map<UUID, Products> byId) {
        if (item.id() == null) {
            return PriceUpdateResult.fail(null, null, "Produk tidak ditemukan");
        }
        Products product = byId.get(item.id());
        if (product == null) {
            return PriceUpdateResult.fail(item.id(), null, "Produk tidak ditemukan");
        }
        String name = item.name() == null ? "" : item.name().trim();
        if (name.isEmpty()) {
            return PriceUpdateResult.fail(item.id(), product.getCode(), "Nama kosong");
        }
        if (name.length() > 100) {
            return PriceUpdateResult.fail(item.id(), product.getCode(), "Nama terlalu panjang");
        }
        if (product.isDeleted()) {
            return PriceUpdateResult.fail(item.id(), product.getCode(), "Produk sudah dihapus");
        }
        product.setName(name);
        productRepository.save(product);
        return PriceUpdateResult.ok(item.id(), product.getCode());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminProducts", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public Products findOrCreateByBrand(String brand, UUID categoryId) {
        String code = CatalogCodeUtil.toCode(brand);
        return productRepository.findByCategoryIdAndCode(categoryId, code).map(existing -> {
            if (existing.isDeleted()) {          // revive soft-deleted, jangan biarin stale
                existing.setDeleted(false);
                existing.setActive(true);
                return productRepository.save(existing);
            }
            return existing;
        }).orElseGet(() -> {
            Products p = new Products();
            p.setCode(code); p.setName(brand); p.setCategoryId(categoryId);
            p.setActive(true); p.setDeleted(false);
            return productRepository.save(p);
        });
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminProducts", allEntries = true, cacheManager = "catalogCacheManager"),
        @CacheEvict(value = "adminActiveDenoms", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public void softDelete(UUID id) {
        Products product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        // Cascade: soft-delete all denoms belonging to this product
        List<ProductDenoms> denoms = denomRepository.findByProductIdOrderBySortOrder(product.getId());
        for (ProductDenoms denom : denoms) {
            if (!denom.isDeleted()) {
                denom.setDeleted(true);
                denom.setActive(false);
                denomRepository.save(denom);
            }
        }
        product.setDeleted(true);
        product.setActive(false);
        productRepository.save(product);
    }

    /**
     * Batch: set flag inSupplier semua produk hidup dalam 1 load (bukan query per kategori).
     * {@code supplierCodesByCategory}: categoryId -> set code brand yang ada di supplier.
     * Produk yang kategorinya tak ada di map dianggap tak ada di supplier (flag false).
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminProducts", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public int reconcileSupplierFlags(Map<UUID, Set<String>> supplierCodesByCategory) {
        int changed = 0;
        for (Products p : productRepository.findByDeletedFalseOrderBySortOrder()) {
            boolean present = supplierCodesByCategory
                    .getOrDefault(p.getCategoryId(), Set.of()).contains(p.getCode());
            if (p.isInSupplier() != present) { p.setInSupplier(present); productRepository.save(p); changed++; }
        }
        return changed;
    }
}