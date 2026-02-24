package com.omnip.catalog.domain.port.out;

import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {

    List<Categories> findByActiveTrueAndDeletedFalseOrderBySortOrder();

    Optional<Categories> findByCode(String code);

    List<Categories> findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(CategoryType categoryType);
}
