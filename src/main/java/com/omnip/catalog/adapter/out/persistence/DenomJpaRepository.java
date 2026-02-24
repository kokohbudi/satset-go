package com.omnip.catalog.adapter.out.persistence;

import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.model.DenomType;
import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DenomJpaRepository extends JpaRepository<ProductDenoms, UUID>, DenomRepositoryPort {

    Optional<ProductDenoms> findByCode(String code);

    List<ProductDenoms> findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID productId);

    List<ProductDenoms> findByProductIdAndDenomTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(
            UUID productId, DenomType denomType);
}
