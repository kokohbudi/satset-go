package com.satset.catalog.adapter.out.persistence;

import com.satset.catalog.domain.model.ProductDenomMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DenomMetaRepository extends JpaRepository<ProductDenomMeta, UUID> {

    List<ProductDenomMeta> findByProductDenomId(UUID productDenomId);
}
