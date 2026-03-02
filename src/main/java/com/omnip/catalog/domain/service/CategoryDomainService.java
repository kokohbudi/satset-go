package com.omnip.catalog.domain.service;

import com.omnip.catalog.domain.port.in.CreateCategoryRequest;
import com.omnip.catalog.domain.port.in.UpdateCategoryRequest;
import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;
import com.omnip.catalog.domain.port.in.BrowseCategoriesUseCase;
import com.omnip.catalog.domain.port.in.ManageCategoriesUseCase;
import com.omnip.catalog.domain.port.out.CategoryRepositoryPort;
import com.omnip.shared.exception.BusinessException;
import com.omnip.shared.exception.ResourceNotFoundException;
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
public class CategoryDomainService implements BrowseCategoriesUseCase, ManageCategoriesUseCase {

    private final CategoryRepositoryPort categoryRepository;

    public CategoryDomainService(CategoryRepositoryPort categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // === Browse (read-only, cached) ===

    @Override
    @Cacheable(value = "categoriesAll", cacheManager = "standardCacheManager")
    public List<Categories> findAll() {
        return categoryRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder();
    }

    @Override
    public Optional<Categories> findByCode(String code) {
        return categoryRepository.findByCode(code)
                .filter(c -> c.isActive() && !c.isDeleted());
    }

    @Override
    @Cacheable(value = "categoriesByType", key = "#type", cacheManager = "standardCacheManager")
    public List<Categories> findByType(CategoryType type) {
        return categoryRepository.findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(type);
    }

    // === Manage (admin CRUD) ===

    @Override
    public List<Categories> findAllForAdmin() {
        return categoryRepository.findAllByOrderBySortOrder();
    }

    @Override
    public Optional<Categories> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager")
    })
    public Categories create(CreateCategoryRequest req) throws BusinessException {
        if (categoryRepository.findByCode(req.code().toUpperCase().trim()).isPresent()) {
            throw new BusinessException("DUPLICATE_CODE", "Category code already exists: " + req.code());
        }
        Categories cat = new Categories();
        cat.setCode(req.code().toUpperCase().trim());
        cat.setName(req.name());
        cat.setCategoryType(req.categoryType());
        cat.setIconUrl(req.iconUrl());
        cat.setActive(req.active());
        cat.setSortOrder(req.sortOrder());
        cat.setDeleted(false);
        return categoryRepository.save(cat);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager")
    })
    public Categories update(UUID id, UpdateCategoryRequest req) throws BusinessException {
        Categories cat = categoryRepository.findById(id)
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

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "categoriesAll", allEntries = true, cacheManager = "standardCacheManager"),
        @CacheEvict(value = "categoriesByType", allEntries = true, cacheManager = "standardCacheManager")
    })
    public void softDelete(UUID id) {
        Categories cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        cat.setDeleted(true);
        cat.setActive(false);
        categoryRepository.save(cat);
    }
}