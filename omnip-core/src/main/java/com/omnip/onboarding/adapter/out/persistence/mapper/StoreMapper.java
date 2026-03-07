package com.omnip.onboarding.adapter.out.persistence.mapper;

import com.omnip.onboarding.adapter.out.persistence.entity.StoreJpaEntity;
import com.omnip.onboarding.domain.model.Stores;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class StoreMapper {

    public Stores toDomain(StoreJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Stores store = new Stores();
        store.setId(entity.getId());
        store.setName(entity.getName());
        store.setReferralId(entity.getReferralId());
        store.setEmail(entity.getEmail());
        store.setPhone(entity.getPhone());
        store.setBalance(entity.getBalance());
        store.setKeycloakOrganizationId(entity.getKeycloakOrganizationId());
        store.setActive(entity.isActive());
        store.setDeleted(entity.isDeleted());
        store.setCreatedAt(entity.getCreatedAt());
        store.setUpdatedAt(entity.getUpdatedAt());
        store.setCreatedBy(entity.getCreatedBy());
        store.setUpdatedBy(entity.getUpdatedBy());
        store.setVersion(entity.getVersion());
        // Convert upline entity to uplineId
        if (entity.getUpline() != null) {
            store.setUplineId(entity.getUpline().getId());
        }
        return store;
    }

    public StoreJpaEntity toEntity(Stores store) {
        if (store == null) {
            return null;
        }
        StoreJpaEntity entity = new StoreJpaEntity();
        entity.setId(store.getId());
        entity.setName(store.getName());
        entity.setReferralId(store.getReferralId());
        entity.setEmail(store.getEmail());
        entity.setPhone(store.getPhone());
        entity.setBalance(store.getBalance());
        entity.setKeycloakOrganizationId(store.getKeycloakOrganizationId());
        entity.setActive(store.isActive());
        entity.setDeleted(store.isDeleted());
        entity.setCreatedAt(store.getCreatedAt());
        entity.setUpdatedAt(store.getUpdatedAt());
        entity.setCreatedBy(store.getCreatedBy());
        entity.setUpdatedBy(store.getUpdatedBy());
        entity.setVersion(store.getVersion());
        // upline will be set separately if needed via uplineId
        return entity;
    }

    public Optional<Stores> toOptionalDomain(Optional<StoreJpaEntity> entity) {
        return entity.map(this::toDomain);
    }

    public List<Stores> toDomainList(List<StoreJpaEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }
}