package com.satset.catalog.repository;

import com.satset.catalog.model.ProductDenoms;
import com.satset.shared.model.DenomInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DenomRepository extends JpaRepository<ProductDenoms, UUID> {

    Optional<ProductDenoms> findByCode(String code);

    List<ProductDenoms> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId);

    List<ProductDenoms> findByProductIdOrderBySortOrder(UUID productId);

    List<ProductDenoms> findAllByOrderBySortOrder();

    boolean existsByCodeAndIdNot(String code, UUID id);

    /**
     * Find denom info by ID for cross-context use.
     * Uses JPQL to directly map to DenomInfo value object.
     */
    @Query("""
            SELECT new com.satset.shared.model.DenomInfo(
            d.id, d.code, d.name, p.name, d.price, d.adminFee, d.basePrice, d.active, d.deleted
        )
        FROM ProductDenoms d
        LEFT JOIN Products p ON d.productId = p.id
        WHERE d.id = :id
        """)
    Optional<DenomInfo> findDenomInfoById(UUID id);
}
