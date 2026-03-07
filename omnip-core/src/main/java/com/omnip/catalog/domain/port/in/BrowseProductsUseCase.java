package com.omnip.catalog.domain.port.in;

import com.omnip.catalog.domain.model.Products;

import java.util.List;
import java.util.Optional;

public interface BrowseProductsUseCase {

    List<Products> findByCategory(String categoryCode);

    List<Products> findActiveProducts();

    Optional<Products> findByCode(String code);
}
