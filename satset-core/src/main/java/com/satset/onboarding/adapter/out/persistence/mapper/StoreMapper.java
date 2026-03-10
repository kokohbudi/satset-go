package com.satset.onboarding.adapter.out.persistence.mapper;

import com.satset.onboarding.adapter.out.persistence.entity.StoreJpaEntity;
import com.satset.onboarding.domain.model.Stores;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface StoreMapper {

    @Mapping(target = "uplineId", source = "upline.id")
    Stores toDomain(StoreJpaEntity entity);

    @Mapping(target = "upline", ignore = true)
    StoreJpaEntity toEntity(Stores store);

    List<Stores> toDomainList(List<StoreJpaEntity> entities);

    default Optional<Stores> toOptionalDomain(Optional<StoreJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}
