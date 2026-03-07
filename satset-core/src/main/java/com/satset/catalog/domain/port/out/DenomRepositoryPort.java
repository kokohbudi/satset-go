package com.satset.catalog.domain.port.out;

import com.satset.catalog.domain.model.ProductDenoms;
import com.satset.shared.model.DenomInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Note: findById() is inherited from JpaRepository/CrudRepository.
 */
public interface DenomRepositoryPort {

    ProductDenoms save(ProductDenoms denom);

    Optional<ProductDenoms> findById(UUID id);

    /**
     * Find denom info by ID for cross-context use.
     * Returns a DenomInfo value object instead of domain entity.
     */
    Optional<DenomInfo> findDenomInfoById(UUID id);

    List<ProductDenoms> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId);

    List<ProductDenoms> findByProductIdOrderBySortOrder(UUID productId);

    Optional<ProductDenoms> findByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
