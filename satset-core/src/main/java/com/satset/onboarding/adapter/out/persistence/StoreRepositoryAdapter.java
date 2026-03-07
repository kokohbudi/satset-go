package com.satset.onboarding.adapter.out.persistence;

import com.satset.onboarding.adapter.out.persistence.entity.StoreJpaEntity;
import com.satset.onboarding.adapter.out.persistence.mapper.StoreMapper;
import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.out.StoreRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class StoreRepositoryAdapter implements StoreRepositoryPort {

    private final StoreJpaRepository jpaRepository;
    private final StoreMapper mapper;

    StoreRepositoryAdapter(StoreJpaRepository jpaRepository, StoreMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public boolean existsByReferralId(String referralId) {
        return jpaRepository.existsByReferralId(referralId);
    }

    @Override
    public Optional<Stores> findById(UUID id) {
        return mapper.toOptionalDomain(jpaRepository.findById(id));
    }

    @Override
    public Stores save(Stores store) {
        StoreJpaEntity entity = mapper.toEntity(store);
        
        // Handle upline relationship if uplineId is set
        if (store.getUplineId() != null) {
            StoreJpaEntity uplineEntity = jpaRepository.findById(store.getUplineId()).orElse(null);
            entity.setUpline(uplineEntity);
        }
        
        StoreJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Stores findByEmail(String email) {
        return mapper.toDomain(jpaRepository.findByEmail(email));
    }

    @Override
    public List<Stores> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }
}