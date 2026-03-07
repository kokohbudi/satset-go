package com.satset.catalog.adapter.out.persistence.mapper;

import com.satset.catalog.adapter.out.persistence.entity.ProductJpaEntity;
import com.satset.catalog.domain.model.Products;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProductMapper {

    public Products toDomain(ProductJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Products product = new Products();
        product.setId(entity.getId());
        product.setCategoryId(entity.getCategoryId());
        product.setCode(entity.getCode());
        product.setName(entity.getName());
        product.setProviderName(entity.getProviderName());
        product.setDescription(entity.getDescription());
        product.setIconUrl(entity.getIconUrl());
        product.setActive(entity.isActive());
        product.setDeleted(entity.isDeleted());
        product.setSortOrder(entity.getSortOrder());
        product.setCreatedAt(entity.getCreatedAt());
        product.setUpdatedAt(entity.getUpdatedAt());
        product.setCreatedBy(entity.getCreatedBy());
        product.setUpdatedBy(entity.getUpdatedBy());
        product.setVersion(entity.getVersion());
        return product;
    }

    public ProductJpaEntity toEntity(Products product) {
        if (product == null) {
            return null;
        }
        ProductJpaEntity entity = new ProductJpaEntity();
        entity.setId(product.getId());
        entity.setCategoryId(product.getCategoryId());
        entity.setCode(product.getCode());
        entity.setName(product.getName());
        entity.setProviderName(product.getProviderName());
        entity.setDescription(product.getDescription());
        entity.setIconUrl(product.getIconUrl());
        entity.setActive(product.isActive());
        entity.setDeleted(product.isDeleted());
        entity.setSortOrder(product.getSortOrder());
        entity.setCreatedAt(product.getCreatedAt());
        entity.setUpdatedAt(product.getUpdatedAt());
        entity.setCreatedBy(product.getCreatedBy());
        entity.setUpdatedBy(product.getUpdatedBy());
        entity.setVersion(product.getVersion());
        return entity;
    }

    public List<Products> toDomainList(List<ProductJpaEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    public Optional<Products> toOptionalDomain(Optional<ProductJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}