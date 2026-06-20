package com.satset.catalog.service;

import com.satset.catalog.repository.CategoryRepository;
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
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoryDomainService {

    private final CategoryRepository categoryRepository;

    public CategoryDomainService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
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

    public List<Category> findAllForAdmin() {
        return categoryRepository.findAllByOrderBySortOrder();
    }

    public Optional<Category> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager")
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
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager")
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
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager")
    })
    public void softDelete(UUID id) {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        cat.setDeleted(true);
        cat.setActive(false);
        categoryRepository.save(cat);
    }
}