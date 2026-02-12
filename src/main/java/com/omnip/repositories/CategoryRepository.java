package com.omnip.repositories;

import com.omnip.entities.Categories;
import com.omnip.enums.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Categories, UUID> {

    Optional<Categories> findByCode(String code);

    List<Categories> findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(CategoryType categoryType);

    List<Categories> findByActiveTrueAndDeletedFalseOrderBySortOrder();
}
