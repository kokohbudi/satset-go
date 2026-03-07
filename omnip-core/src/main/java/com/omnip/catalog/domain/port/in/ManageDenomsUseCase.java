package com.omnip.catalog.domain.port.in;

import com.omnip.catalog.domain.port.in.CreateDenomRequest;
import com.omnip.catalog.domain.port.in.UpdateDenomRequest;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.shared.exception.BusinessException;

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
