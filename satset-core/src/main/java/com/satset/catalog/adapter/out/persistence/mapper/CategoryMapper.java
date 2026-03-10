package com.satset.catalog.adapter.out.persistence.mapper;

import com.satset.catalog.adapter.out.persistence.entity.CategoryJpaEntity;
import com.satset.catalog.domain.model.Category;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toDomain(CategoryJpaEntity entity);

    CategoryJpaEntity toEntity(Category category);

    List<Category> toDomainList(List<CategoryJpaEntity> entities);

    default Optional<Category> toOptionalDomain(Optional<CategoryJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}
