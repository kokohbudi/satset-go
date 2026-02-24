package com.omnip.catalog.adapter.out.persistence;

import com.omnip.catalog.domain.model.Products;
import com.omnip.catalog.domain.port.out.ProductRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<Products, UUID>, ProductRepositoryPort {

    Optional<Products> findByCode(String code);

    List<Products> findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID categoryId);

    List<Products> findByActiveTrueAndDeletedFalseOrderBySortOrder();
}
