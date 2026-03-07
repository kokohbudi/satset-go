package com.satset.catalog.adapter.out.persistence.mapper;

import com.satset.catalog.adapter.out.persistence.entity.CategoryJpaEntity;
import com.satset.catalog.domain.model.Category;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CategoryMapper {

    public Category toDomain(CategoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Category category = new Category();
        category.setId(entity.getId());
        category.setCode(entity.getCode());
        category.setName(entity.getName());
        category.setCategoryType(entity.getCategoryType());
        category.setIconUrl(entity.getIconUrl());
        category.setActive(entity.isActive());
        category.setDeleted(entity.isDeleted());
        category.setSortOrder(entity.getSortOrder());
        category.setCreatedAt(entity.getCreatedAt());
        category.setUpdatedAt(entity.getUpdatedAt());
        category.setCreatedBy(entity.getCreatedBy());
        category.setUpdatedBy(entity.getUpdatedBy());
        category.setVersion(entity.getVersion());
        return category;
    }

    public CategoryJpaEntity toEntity(Category category) {
        if (category == null) {
            return null;
        }
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(category.getId());
        entity.setCode(category.getCode());
        entity.setName(category.getName());
        entity.setCategoryType(category.getCategoryType());
        entity.setIconUrl(category.getIconUrl());
        entity.setActive(category.isActive());
        entity.setDeleted(category.isDeleted());
        entity.setSortOrder(category.getSortOrder());
        entity.setCreatedAt(category.getCreatedAt());
        entity.setUpdatedAt(category.getUpdatedAt());
        entity.setCreatedBy(category.getCreatedBy());
        entity.setUpdatedBy(category.getUpdatedBy());
        entity.setVersion(category.getVersion());
        return entity;
    }

    public List<Category> toDomainList(List<CategoryJpaEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    public Optional<Category> toOptionalDomain(Optional<CategoryJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}