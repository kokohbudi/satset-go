package com.omnip.catalog.domain.port.in;

import com.omnip.catalog.domain.port.in.CreateCategoryRequest;
import com.omnip.catalog.domain.port.in.UpdateCategoryRequest;
import com.omnip.catalog.domain.model.Categories;
import com.omnip.shared.exception.BusinessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageCategoriesUseCase {

    List<Categories> findAllForAdmin();

    Optional<Categories> findById(UUID id);

    Categories create(CreateCategoryRequest req) throws BusinessException;

    Categories update(UUID id, UpdateCategoryRequest req) throws BusinessException;

    void softDelete(UUID id);
}
