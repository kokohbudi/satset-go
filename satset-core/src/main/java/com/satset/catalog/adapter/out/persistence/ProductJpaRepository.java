package com.satset.catalog.adapter.out.persistence;

import com.satset.catalog.adapter.out.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Optional<ProductJpaEntity> findByCode(String code);

    List<ProductJpaEntity> findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID categoryId);

    List<ProductJpaEntity> findByCategoryIdOrderBySortOrder(UUID categoryId);

    List<ProductJpaEntity> findByActiveTrueAndDeletedFalseOrderBySortOrder();

    boolean existsByCodeAndIdNot(String code, UUID id);
}