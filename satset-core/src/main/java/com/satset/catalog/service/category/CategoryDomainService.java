package com.satset.catalog.service.category;

import com.satset.catalog.repository.CategoryRepository;
import com.satset.catalog.repository.ProductRepository;
import com.satset.catalog.service.CatalogCodeUtil;
import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import com.satset.catalog.dto.CreateCategoryRequest;
import com.satset.catalog.dto.UpdateCategoryRequest;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoryDomainService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryDomainService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    // === Browse (read-only, cached) ===

    @Cacheable(value = "categoriesAll", cacheManager = "standardCacheManager")
    public List<Category> findAll() {
        return categoryRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder();
    }

    public Optional<Category> findByCode(String code) {
        return categoryRepository.findByCode(code)
                .filter(c -> c.isActive() && !c.isDeleted());
    }

    @Cacheable(value = "categoriesByType", key = "#type", cacheManager = "standardCacheManager")
    public List<Category> findByType(CategoryType type) {
        return categoryRepository.findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(type);
    }

    // === Manage (admin CRUD) ===

    /** Semua kategori (incl. deleted) buat admin + sync batch. Cached tanpa TTL — evict tiap mutasi kategori. */
    @Cacheable(value = "adminCategories", cacheManager = "catalogCacheManager")
    public List<Category> findAllForAdmin() {
        return categoryRepository.findAllByOrderBySortOrder();
    }

    public Optional<Category> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminCategories", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public Category create(CreateCategoryRequest req) throws BusinessException {
        if (categoryRepository.findByCode(req.code().toUpperCase().trim()).isPresent()) {
            throw new BusinessException("DUPLICATE_CODE", "Category code already exists: " + req.code());
        }
        Category cat = new Category();
        cat.setCode(req.code().toUpperCase().trim());
        cat.setName(req.name());
        cat.setCategoryType(req.categoryType());
        cat.setIconUrl(req.iconUrl());
        cat.setActive(req.active());
        cat.setSortOrder(req.sortOrder());
        cat.setDeleted(false);
        return categoryRepository.save(cat);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminCategories", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public Category update(UUID id, UpdateCategoryRequest req) throws BusinessException {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (categoryRepository.existsByCodeAndIdNot(req.code().toUpperCase().trim(), id)) {
            throw new BusinessException("DUPLICATE_CODE", "Category code already exists: " + req.code());
        }
        cat.setCode(req.code().toUpperCase().trim());
        cat.setName(req.name());
        cat.setCategoryType(req.categoryType());
        cat.setIconUrl(req.iconUrl());
        cat.setActive(req.active());
        cat.setSortOrder(req.sortOrder());
        return categoryRepository.save(cat);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminCategories", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public Category findOrCreateByName(String dfName) {
        String code = CatalogCodeUtil.toCode(dfName);
        return categoryRepository.findByCode(code).map(existing -> {
            if (existing.isDeleted()) {          // revive soft-deleted, jangan biarin stale
                existing.setDeleted(false);
                existing.setActive(true);
                return categoryRepository.save(existing);
            }
            return existing;
        }).orElseGet(() -> {
            Category c = new Category();
            c.setCode(code); c.setName(dfName);
            c.setCategoryType(CategoryType.PREPAID);
            c.setActive(true); c.setDeleted(false);
            return categoryRepository.save(c);
        });
    }

    /** Set flag inSupplier tiap kategori hidup: true kalau code-nya ada di {@code supplierCodes}. */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminCategories", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public int reconcileSupplierFlags(Set<String> supplierCodes) {
        int changed = 0;
        for (Category c : categoryRepository.findAllByOrderBySortOrder()) {
            if (c.isDeleted()) continue;
            boolean present = supplierCodes.contains(c.getCode());
            if (c.isInSupplier() != present) { c.setInSupplier(present); categoryRepository.save(c); changed++; }
        }
        return changed;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "adminCategories", allEntries = true, cacheManager = "catalogCacheManager")
    })
    public void softDelete(UUID id) throws BusinessException {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (productRepository.existsByCategoryIdAndDeletedFalse(id)) {
            throw new BusinessException("CATEGORY_HAS_PRODUCTS",
                "Kategori masih punya produk aktif; pindahkan atau hapus produknya dulu");
        }
        cat.setDeleted(true);
        cat.setActive(false);
        categoryRepository.save(cat);
    }
}