package com.satset.catalog.repository;

import com.satset.catalog.model.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Products, UUID> {

    Optional<Products> findByCode(String code);

    List<Products> findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID categoryId);

    List<Products> findByCategoryIdOrderBySortOrder(UUID categoryId);

    boolean existsByCategoryIdAndDeletedFalse(UUID categoryId);

    List<Products> findByActiveTrueAndDeletedFalseOrderBySortOrder();

    boolean existsByCodeAndIdNot(String code, UUID id);
}
