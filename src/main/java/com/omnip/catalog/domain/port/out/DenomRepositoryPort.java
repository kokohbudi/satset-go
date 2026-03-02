package com.omnip.catalog.domain.port.out;

import com.omnip.catalog.domain.model.ProductDenoms;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Note: findById() is inherited from JpaRepository/CrudRepository.
 */
public interface DenomRepositoryPort {

    ProductDenoms save(ProductDenoms denom);

    Optional<ProductDenoms> findById(UUID id);

    List<ProductDenoms> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId);

    List<ProductDenoms> findByProductIdOrderBySortOrder(UUID productId);

    Optional<ProductDenoms> findByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
