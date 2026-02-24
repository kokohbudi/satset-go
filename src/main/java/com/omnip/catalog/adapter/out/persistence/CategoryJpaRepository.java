package com.omnip.catalog.adapter.out.persistence;

import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryJpaRepository extends JpaRepository<Categories, UUID> {

    Optional<Categories> findByCode(String code);

    List<Categories> findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(CategoryType categoryType);

    List<Categories> findByActiveTrueAndDeletedFalseOrderBySortOrder();
}
