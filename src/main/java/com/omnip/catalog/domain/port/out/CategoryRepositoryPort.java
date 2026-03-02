package com.omnip.catalog.domain.port.out;

import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {

    Categories save(Categories category);

    long count();

    Optional<Categories> findById(UUID id);

    List<Categories> findByActiveTrueAndDeletedFalseOrderBySortOrder();

    List<Categories> findAllByOrderBySortOrder();

    Optional<Categories> findByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Categories> findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(CategoryType categoryType);
}
