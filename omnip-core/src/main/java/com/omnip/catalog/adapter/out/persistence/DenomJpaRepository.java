package com.omnip.catalog.adapter.out.persistence;

import com.omnip.catalog.adapter.out.persistence.entity.ProductDenomJpaEntity;
import com.omnip.shared.model.DenomInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DenomJpaRepository extends JpaRepository<ProductDenomJpaEntity, UUID> {

    Optional<ProductDenomJpaEntity> findByCode(String code);

    List<ProductDenomJpaEntity> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId);

    List<ProductDenomJpaEntity> findByProductIdOrderBySortOrder(UUID productId);

    boolean existsByCodeAndIdNot(String code, UUID id);

    /**
     * Find denom info by ID for cross-context use.
     * Uses JPQL to directly map to DenomInfo value object.
     */
    @Query("""
        SELECT new com.omnip.shared.model.DenomInfo(
            d.id, d.code, d.name, p.name, d.price, d.adminFee, d.active, d.deleted
        )
        FROM ProductDenomJpaEntity d
        LEFT JOIN ProductJpaEntity p ON d.productId = p.id
        WHERE d.id = :id
        """)
    Optional<DenomInfo> findDenomInfoById(UUID id);
}