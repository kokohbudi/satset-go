package com.satset.catalog.domain.port.in;

import com.satset.catalog.domain.model.Category;
import com.satset.shared.exception.BusinessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManageCategoriesUseCase {

    List<Category> findAllForAdmin();

    Optional<Category> findById(UUID id);

    Category create(CreateCategoryRequest req) throws BusinessException;

    Category update(UUID id, UpdateCategoryRequest req) throws BusinessException;

    void softDelete(UUID id);
}
