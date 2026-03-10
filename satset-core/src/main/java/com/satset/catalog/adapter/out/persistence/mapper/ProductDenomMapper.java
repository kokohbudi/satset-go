package com.satset.catalog.adapter.out.persistence.mapper;

import com.satset.catalog.adapter.out.persistence.entity.ProductDenomJpaEntity;
import com.satset.catalog.domain.model.ProductDenoms;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface ProductDenomMapper {

    ProductDenoms toDomain(ProductDenomJpaEntity entity);

    ProductDenomJpaEntity toEntity(ProductDenoms denom);

    List<ProductDenoms> toDomainList(List<ProductDenomJpaEntity> entities);

    default Optional<ProductDenoms> toOptionalDomain(Optional<ProductDenomJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}
