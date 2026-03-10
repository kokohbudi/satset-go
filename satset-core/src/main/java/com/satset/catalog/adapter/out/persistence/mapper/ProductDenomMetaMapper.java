package com.satset.catalog.adapter.out.persistence.mapper;

import com.satset.catalog.adapter.out.persistence.entity.ProductDenomMetaJpaEntity;
import com.satset.catalog.domain.model.ProductDenomMeta;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductDenomMetaMapper {

    ProductDenomMeta toDomain(ProductDenomMetaJpaEntity entity);

    ProductDenomMetaJpaEntity toEntity(ProductDenomMeta meta);

    List<ProductDenomMeta> toDomainList(List<ProductDenomMetaJpaEntity> entities);
}
