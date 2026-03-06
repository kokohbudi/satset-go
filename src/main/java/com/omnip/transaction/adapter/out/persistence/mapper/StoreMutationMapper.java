package com.omnip.transaction.adapter.out.persistence.mapper;

import com.omnip.transaction.adapter.out.persistence.entity.StoreMutationJpaEntity;
import com.omnip.transaction.domain.model.StoreMutations;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StoreMutationMapper {

    public StoreMutations toDomain(StoreMutationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        StoreMutations mutation = new StoreMutations();
        mutation.setId(entity.getId());
        mutation.setStoreId(entity.getStoreId());
        mutation.setAmount(entity.getAmount());
        mutation.setType(entity.getType());
        mutation.setBalanceAfter(entity.getBalanceAfter());
        mutation.setReferenceType(entity.getReferenceType());
        mutation.setReferenceId(entity.getReferenceId());
        mutation.setDescription(entity.getDescription());
        mutation.setCreatedAt(entity.getCreatedAt());
        mutation.setVersion(entity.getVersion());
        return mutation;
    }

    public StoreMutationJpaEntity toEntity(StoreMutations mutation) {
        if (mutation == null) {
            return null;
        }
        StoreMutationJpaEntity entity = new StoreMutationJpaEntity();
        entity.setId(mutation.getId());
        entity.setStoreId(mutation.getStoreId());
        entity.setAmount(mutation.getAmount());
        entity.setType(mutation.getType());
        entity.setBalanceAfter(mutation.getBalanceAfter());
        entity.setReferenceType(mutation.getReferenceType());
        entity.setReferenceId(mutation.getReferenceId());
        entity.setDescription(mutation.getDescription());
        entity.setCreatedAt(mutation.getCreatedAt());
        entity.setVersion(mutation.getVersion());
        return entity;
    }

    public Optional<StoreMutations> toOptionalDomain(Optional<StoreMutationJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}