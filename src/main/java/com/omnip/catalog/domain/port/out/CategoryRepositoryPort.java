package com.omnip.catalog.domain.port.out;

import com.omnip.catalog.domain.model.Category;
import com.omnip.catalog.domain.model.CategoryType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {

    Category save(Category category);

    long count();

    Optional<Category> findById(UUID id);

    List<Category> findByActiveTrueAndDeletedFalseOrderBySortOrder();

    List<Category> findAllByOrderBySortOrder();

    Optional<Category> findByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Category> findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(CategoryType categoryType);
}
