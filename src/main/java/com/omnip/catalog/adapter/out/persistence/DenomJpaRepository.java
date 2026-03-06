package com.omnip.catalog.adapter.out.persistence;

import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.model.DenomType;
import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import com.omnip.shared.model.DenomInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DenomJpaRepository extends JpaRepository<ProductDenoms, UUID>, DenomRepositoryPort {

    Optional<ProductDenoms> findByCode(String code);

    List<ProductDenoms> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId);

    List<ProductDenoms> findByProductIdOrderBySortOrder(UUID productId);

    boolean existsByCodeAndIdNot(String code, UUID id);

    /**
     * Find denom info by ID for cross-context use.
     * Uses JPQL to directly map to DenomInfo value object.
     */
    @Query("""
        SELECT new com.omnip.shared.model.DenomInfo(
            d.id, d.code, d.name, p.name, d.price, d.adminFee, d.active, d.deleted
        )
        FROM ProductDenoms d
        LEFT JOIN d.product p
        WHERE d.id = :id
        """)
    Optional<DenomInfo> findDenomInfoById(UUID id);
}
