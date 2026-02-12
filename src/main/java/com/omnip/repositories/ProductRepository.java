package com.omnip.repositories;

import com.omnip.entities.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Products, UUID> {

    Optional<Products> findByCode(String code);

    List<Products> findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(UUID categoryId);

    List<Products> findByActiveTrueAndDeletedFalseOrderBySortOrder();
}
