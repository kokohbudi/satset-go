package com.omnip.catalog.domain.port.in;

import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;

import java.util.List;
import java.util.Optional;

public interface BrowseCategoriesUseCase {

    List<Categories> findAll();

    Optional<Categories> findByCode(String code);

    List<Categories> findByType(CategoryType type);
}
