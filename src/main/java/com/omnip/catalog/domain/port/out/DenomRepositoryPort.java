package com.omnip.catalog.domain.port.out;

import com.omnip.catalog.domain.model.ProductDenoms;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DenomRepositoryPort {

    List<ProductDenoms> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId);

    Optional<ProductDenoms> findByCode(String code);

    Optional<ProductDenoms> findById(UUID id);
}
