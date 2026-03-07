package com.satset.catalog.domain.port.in;

import com.satset.catalog.domain.model.Products;
import com.satset.shared.exception.BusinessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageProductsUseCase {

    List<Products> findByCategoryForAdmin(UUID categoryId);

    Optional<Products> findById(UUID id);

    Products create(CreateProductRequest req) throws BusinessException;

    Products update(UUID id, UpdateProductRequest req) throws BusinessException;

    void softDelete(UUID id);
}
