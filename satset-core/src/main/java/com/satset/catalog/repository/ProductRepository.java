package com.satset.catalog.repository;

import com.satset.catalog.model.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Products, UUID> {

    Optional<Products> findByCategoryIdAndCode(UUID categoryId, String code);

    boolean existsByCategoryIdAndCodeAndIdNot(UUID categoryId, String code, UUID id);

    List<Products> findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID categoryId);

    List<Products> findByCategoryIdOrderBySortOrder(UUID categoryId);

    boolean existsByCategoryIdAndDeletedFalse(UUID categoryId);

    List<Products> findByActiveTrueAndDeletedFalseOrderBySortOrder();
}
