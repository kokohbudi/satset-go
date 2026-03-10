package com.satset.transaction.adapter.out.persistence.mapper;

import com.satset.transaction.adapter.out.persistence.entity.StoreMutationJpaEntity;
import com.satset.transaction.domain.model.StoreMutations;
import org.mapstruct.Mapper;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface StoreMutationMapper {

    StoreMutations toDomain(StoreMutationJpaEntity entity);

    StoreMutationJpaEntity toEntity(StoreMutations mutation);

    default Optional<StoreMutations> toOptionalDomain(Optional<StoreMutationJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}
