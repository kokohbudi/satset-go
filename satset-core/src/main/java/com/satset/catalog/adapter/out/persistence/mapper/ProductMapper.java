package com.satset.catalog.adapter.out.persistence.mapper;

import com.satset.catalog.adapter.out.persistence.entity.ProductJpaEntity;
import com.satset.catalog.domain.model.Products;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    Products toDomain(ProductJpaEntity entity);

    ProductJpaEntity toEntity(Products product);

    List<Products> toDomainList(List<ProductJpaEntity> entities);

    default Optional<Products> toOptionalDomain(Optional<ProductJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}
