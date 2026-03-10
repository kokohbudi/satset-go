package com.satset.transaction.adapter.out.persistence;

import com.satset.transaction.adapter.out.persistence.entity.StoreMutationJpaEntity;
import com.satset.transaction.adapter.out.persistence.mapper.StoreMutationMapper;
import com.satset.transaction.domain.model.StoreMutations;
import com.satset.transaction.domain.port.out.StoreMutationRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class StoreMutationRepositoryAdapter implements StoreMutationRepositoryPort {

    private final StoreMutationJpaRepository jpaRepository;
    private final StoreMutationMapper mapper;

    StoreMutationRepositoryAdapter(StoreMutationJpaRepository jpaRepository, StoreMutationMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public StoreMutations save(StoreMutations mutation) {
        StoreMutationJpaEntity entity = mapper.toEntity(mutation);
        StoreMutationJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<StoreMutations> findTopByWalletIdOrderByCreatedAtDesc(String walletId) {
        return mapper.toOptionalDomain(jpaRepository.findTopByWalletIdOrderByCreatedAtDesc(walletId));
    }
}
