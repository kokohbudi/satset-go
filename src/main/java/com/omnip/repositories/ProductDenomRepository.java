package com.omnip.repositories;

import com.omnip.entities.ProductDenoms;
import com.omnip.enums.DenomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductDenomRepository extends JpaRepository<ProductDenoms, UUID> {

    Optional<ProductDenoms> findByCode(String code);

    List<ProductDenoms> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId);

    List<ProductDenoms> findByProductIdAndDenomTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(
            UUID productId, DenomType denomType);
}
