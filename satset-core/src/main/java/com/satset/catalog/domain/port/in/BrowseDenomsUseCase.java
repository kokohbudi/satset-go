package com.satset.catalog.domain.port.in;

import com.satset.catalog.domain.model.ProductDenoms;

import java.util.List;
import java.util.Optional;

public interface BrowseDenomsUseCase {

    List<ProductDenoms> findByProduct(String productCode);

    Optional<ProductDenoms> findByCode(String code);

    Optional<ProductDenoms> getDenomWithMeta(String code);
}
