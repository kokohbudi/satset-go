package com.satset.catalog.domain.port.in;

import com.satset.catalog.domain.model.ProductDenoms;
import com.satset.shared.exception.BusinessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageDenomsUseCase {

    List<ProductDenoms> findByProductForAdmin(UUID productId);

    Optional<ProductDenoms> findById(UUID id);

    ProductDenoms create(UUID productId, CreateDenomRequest req) throws BusinessException;

    ProductDenoms update(UUID id, UpdateDenomRequest req) throws BusinessException;

    void softDelete(UUID id);
}
