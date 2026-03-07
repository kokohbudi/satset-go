package com.omnip.catalog.domain.port.out;

import com.omnip.catalog.domain.model.Products;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {

    Products save(Products product);

    Optional<Products> findById(UUID id);

    List<Products> findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID categoryId);

    List<Products> findByCategoryIdOrderBySortOrder(UUID categoryId);

    List<Products> findByActiveTrueAndDeletedFalseOrderBySortOrder();

    Optional<Products> findByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}
