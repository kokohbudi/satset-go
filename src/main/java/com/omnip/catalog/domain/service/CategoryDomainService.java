package com.omnip.catalog.domain.service;

import com.omnip.catalog.adapter.in.web.dto.CategoryDTO;
import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;
import com.omnip.catalog.adapter.out.persistence.CategoryJpaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CategoryDomainService {

    private final CategoryJpaRepository categoryRepository;

    public CategoryDomainService(CategoryJpaRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Cacheable(value = "categories", cacheManager = "standardCacheManager")
    public List<CategoryDTO> findAll() {
        return categoryRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<CategoryDTO> findByCode(String code) {
        return categoryRepository.findByCode(code)
                .filter(c -> c.isActive() && !c.isDeleted())
                .map(this::toDTO);
    }

    @Cacheable(value = "categories", key = "#type", cacheManager = "standardCacheManager")
    public List<CategoryDTO> findByType(CategoryType type) {
        return categoryRepository.findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(type)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private CategoryDTO toDTO(Categories entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setCategoryType(entity.getCategoryType());
        dto.setIconUrl(entity.getIconUrl());
        dto.setSortOrder(entity.getSortOrder());
        return dto;
    }
}