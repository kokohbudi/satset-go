package com.satset.catalog.adapter.out.persistence.mapper;

import com.satset.catalog.adapter.out.persistence.entity.ProductDenomMetaJpaEntity;
import com.satset.catalog.domain.model.ProductDenomMeta;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductDenomMetaMapper {

    public ProductDenomMeta toDomain(ProductDenomMetaJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        ProductDenomMeta meta = new ProductDenomMeta();
        meta.setId(entity.getId());
        meta.setProductDenomId(entity.getProductDenomId());
        meta.setMetaKey(entity.getMetaKey());
        meta.setMetaValue(entity.getMetaValue());
        meta.setCreatedAt(entity.getCreatedAt());
        return meta;
    }

    public ProductDenomMetaJpaEntity toEntity(ProductDenomMeta meta) {
        if (meta == null) {
            return null;
        }
        ProductDenomMetaJpaEntity entity = new ProductDenomMetaJpaEntity();
        entity.setId(meta.getId());
        entity.setProductDenomId(meta.getProductDenomId());
        entity.setMetaKey(meta.getMetaKey());
        entity.setMetaValue(meta.getMetaValue());
        entity.setCreatedAt(meta.getCreatedAt());
        return entity;
    }

    public List<ProductDenomMeta> toDomainList(List<ProductDenomMetaJpaEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }
}