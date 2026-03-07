package com.satset.catalog.adapter.out.persistence;

import com.satset.catalog.adapter.out.persistence.entity.CategoryJpaEntity;
import com.satset.catalog.adapter.out.persistence.mapper.CategoryMapper;
import com.satset.catalog.domain.model.Category;
import com.satset.catalog.domain.model.CategoryType;
import com.satset.catalog.domain.port.out.CategoryRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class CategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository jpaRepository;
    private final CategoryMapper mapper;

    CategoryRepositoryAdapter(CategoryJpaRepository jpaRepository, CategoryMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = mapper.toEntity(category);
        CategoryJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return mapper.toOptionalDomain(jpaRepository.findById(id));
    }

    @Override
    public List<Category> findByActiveTrueAndDeletedFalseOrderBySortOrder() {
        return mapper.toDomainList(jpaRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder());
    }

    @Override
    public List<Category> findAllByOrderBySortOrder() {
        return mapper.toDomainList(jpaRepository.findAllByOrderBySortOrder());
    }

    @Override
    public Optional<Category> findByCode(String code) {
        return mapper.toOptionalDomain(jpaRepository.findByCode(code));
    }

    @Override
    public boolean existsByCodeAndIdNot(String code, UUID id) {
        return jpaRepository.existsByCodeAndIdNot(code, id);
    }

    @Override
    public List<Category> findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(CategoryType categoryType) {
        return mapper.toDomainList(jpaRepository.findByCategoryTypeAndActiveTrueAndDeletedFalseOrderBySortOrder(categoryType));
    }
}