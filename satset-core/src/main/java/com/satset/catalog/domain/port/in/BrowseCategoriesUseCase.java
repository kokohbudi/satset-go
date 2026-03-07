package com.satset.catalog.domain.port.in;

import com.satset.catalog.domain.model.Category;
import com.satset.catalog.domain.model.CategoryType;

import java.util.List;
import java.util.Optional;

public interface BrowseCategoriesUseCase {

    List<Category> findAll();

    Optional<Category> findByCode(String code);

    List<Category> findByType(CategoryType type);
}
