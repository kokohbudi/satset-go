package com.satset.catalog.repository;

import com.satset.catalog.model.Category;
import com.satset.catalog.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findByCode(String code);

    List<Category> findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(CategoryType categoryType);

    List<Category> findByActiveTrueAndDeletedFalseOrderBySortOrder();

    List<Category> findAllByOrderBySortOrder();

    boolean existsByCodeAndIdNot(String code, UUID id);
}
