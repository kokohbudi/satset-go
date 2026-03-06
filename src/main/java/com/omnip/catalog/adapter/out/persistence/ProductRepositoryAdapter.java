package com.omnip.catalog.adapter.out.persistence;

import com.omnip.catalog.adapter.out.persistence.entity.ProductJpaEntity;
import com.omnip.catalog.adapter.out.persistence.mapper.ProductMapper;
import com.omnip.catalog.domain.model.Products;
import com.omnip.catalog.domain.port.out.ProductRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository jpaRepository;
    private final ProductMapper mapper;

    ProductRepositoryAdapter(ProductJpaRepository jpaRepository, ProductMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Products save(Products product) {
        ProductJpaEntity entity = mapper.toEntity(product);
        ProductJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Products> findById(UUID id) {
        return mapper.toOptionalDomain(jpaRepository.findById(id));
    }

    @Override
    public List<Products> findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID categoryId) {
        return mapper.toDomainList(jpaRepository.findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(categoryId));
    }

    @Override
    public List<Products> findByCategoryIdOrderBySortOrder(UUID categoryId) {
        return mapper.toDomainList(jpaRepository.findByCategoryIdOrderBySortOrder(categoryId));
    }

    @Override
    public List<Products> findByActiveTrueAndDeletedFalseOrderBySortOrder() {
        return mapper.toDomainList(jpaRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder());
    }

    @Override
    public Optional<Products> findByCode(String code) {
        return mapper.toOptionalDomain(jpaRepository.findByCode(code));
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, UUID id) {
        return jpaRepository.existsByCodeAndIdNot(code, id);
    }
}