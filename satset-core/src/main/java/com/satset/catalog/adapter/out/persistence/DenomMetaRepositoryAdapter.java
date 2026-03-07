package com.satset.catalog.adapter.out.persistence;

import com.satset.catalog.adapter.out.persistence.entity.ProductDenomMetaJpaEntity;
import com.satset.catalog.adapter.out.persistence.mapper.ProductDenomMetaMapper;
import com.satset.catalog.domain.model.ProductDenomMeta;
import com.satset.catalog.domain.port.out.DenomMetaRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
class DenomMetaRepositoryAdapter implements DenomMetaRepositoryPort {

    private final DenomMetaJpaRepository jpaRepository;
    private final ProductDenomMetaMapper mapper;

    DenomMetaRepositoryAdapter(DenomMetaJpaRepository jpaRepository, ProductDenomMetaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ProductDenomMeta save(ProductDenomMeta meta) {
        ProductDenomMetaJpaEntity entity = mapper.toEntity(meta);
        ProductDenomMetaJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<ProductDenomMeta> findByProductDenomId(UUID productDenomId) {
        return mapper.toDomainList(jpaRepository.findByProductDenomId(productDenomId));
    }
}