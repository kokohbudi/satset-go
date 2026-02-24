package com.omnip.catalog.adapter.out.persistence;

import com.omnip.catalog.domain.model.ProductDenomMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DenomMetaJpaRepository extends JpaRepository<ProductDenomMeta, UUID> {

    List<ProductDenomMeta> findByProductDenomId(UUID productDenomId);
}
