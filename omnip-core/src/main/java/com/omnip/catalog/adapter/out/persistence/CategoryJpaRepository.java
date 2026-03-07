package com.omnip.catalog.adapter.out.persistence;

import com.omnip.catalog.adapter.out.persistence.entity.CategoryJpaEntity;
import com.omnip.catalog.domain.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    Optional<CategoryJpaEntity> findByCode(String code);

    List<CategoryJpaEntity> findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(CategoryType categoryType);

    List<CategoryJpaEntity> findByActiveTrueAndDeletedFalseOrderBySortOrder();

    List<CategoryJpaEntity> findAllByOrderBySortOrder();

    boolean existsByCodeAndIdNot(String code, UUID id);
}