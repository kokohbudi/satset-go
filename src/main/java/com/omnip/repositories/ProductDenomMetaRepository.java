package com.omnip.repositories;

import com.omnip.entities.ProductDenomMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductDenomMetaRepository extends JpaRepository<ProductDenomMeta, UUID> {

    List<ProductDenomMeta> findByProductDenomId(UUID productDenomId);
}
