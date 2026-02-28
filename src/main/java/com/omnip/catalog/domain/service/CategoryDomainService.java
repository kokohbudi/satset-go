package com.omnip.catalog.domain.service;

import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;
import com.omnip.catalog.domain.port.in.BrowseCategoriesUseCase;
import com.omnip.catalog.domain.port.out.CategoryRepositoryPort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CategoryDomainService implements BrowseCategoriesUseCase {

    private final CategoryRepositoryPort categoryRepository;

    public CategoryDomainService(CategoryRepositoryPort categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

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
}