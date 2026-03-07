package com.omnip.catalog.domain.port.out;

import com.omnip.catalog.domain.model.ProductDenomMeta;

import java.util.List;
import java.util.UUID;

public interface DenomMetaRepositoryPort {

    ProductDenomMeta save(ProductDenomMeta meta);

    List<ProductDenomMeta> findByProductDenomId(UUID productDenomId);
}
