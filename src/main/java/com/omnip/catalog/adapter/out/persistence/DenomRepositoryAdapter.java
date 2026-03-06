package com.omnip.catalog.adapter.out.persistence;

import com.omnip.catalog.adapter.out.persistence.entity.ProductDenomJpaEntity;
import com.omnip.catalog.adapter.out.persistence.mapper.ProductDenomMapper;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import com.omnip.shared.model.DenomInfo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DenomRepositoryAdapter implements DenomRepositoryPort {

    private final DenomJpaRepository jpaRepository;
    private final ProductDenomMapper mapper;

    DenomRepositoryAdapter(DenomJpaRepository jpaRepository, ProductDenomMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ProductDenoms save(ProductDenoms denom) {
        ProductDenomJpaEntity entity = mapper.toEntity(denom);
        ProductDenomJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProductDenoms> findById(UUID id) {
        return mapper.toOptionalDomain(jpaRepository.findById(id));
    }

    @Override
    public Optional<DenomInfo> findDenomInfoById(UUID id) {
        return jpaRepository.findDenomInfoById(id);
    }

    @Override
    public Optional<ProductDenoms> findByCode(String code) {
        return mapper.toOptionalDomain(jpaRepository.findByCode(code));
    }

    @Override
    public List<ProductDenoms> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId) {
        return mapper.toDomainList(jpaRepository.findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(productId));
    }

    @Override
    public List<ProductDenoms> findByProductIdOrderBySortOrder(UUID productId) {
        return mapper.toDomainList(jpaRepository.findByProductIdOrderBySortOrder(productId));
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, UUID id) {
        return jpaRepository.existsByCodeAndIdNot(code, id);
    }
}