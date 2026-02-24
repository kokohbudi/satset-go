package com.omnip.catalog.domain.service;

import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;
import com.omnip.catalog.domain.port.in.BrowseCategoriesUseCase;
import com.omnip.catalog.adapter.out.persistence.CategoryJpaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CategoryDomainService implements BrowseCategoriesUseCase {

    private final CategoryJpaRepository categoryRepository;

    public CategoryDomainService(CategoryJpaRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Cacheable(value = "categories", cacheManager = "standardCacheManager")
    public List<Categories> findAll() {
        return categoryRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder();
    }

    @Override
    public Optional<Categories> findByCode(String code) {
        return categoryRepository.findByCode(code)
                .filter(c -> c.isActive() && !c.isDeleted());
    }

    @Override
    @Cacheable(value = "categories", key = "#type", cacheManager = "standardCacheManager")
    public List<Categories> findByType(CategoryType type) {
        return categoryRepository.findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(type);
    }
}