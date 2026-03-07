package com.omnip.transaction.adapter.out.persistence;

import com.omnip.transaction.adapter.out.persistence.entity.TransactionJpaEntity;
import com.omnip.transaction.adapter.out.persistence.mapper.TransactionMapper;
import com.omnip.transaction.domain.model.TransactionStatus;
import com.omnip.transaction.domain.model.Transactions;
import com.omnip.transaction.domain.port.out.TransactionRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
class TransactionRepositoryAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository jpaRepository;
    private final TransactionMapper mapper;

    TransactionRepositoryAdapter(TransactionJpaRepository jpaRepository, TransactionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Transactions save(Transactions transaction) {
        TransactionJpaEntity entity = mapper.toEntity(transaction);
        TransactionJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Transactions> findByIdAndStoreIdWithDetails(UUID id, UUID storeId) {
        return mapper.toOptionalDomain(jpaRepository.findByIdAndStoreIdWithDetails(id, storeId));
    }

    @Override
    public Page<Transactions> findByStoreIdWithDetails(UUID storeId, Pageable pageable) {
        return mapper.toDomainPage(jpaRepository.findByStoreIdWithDetails(storeId, pageable));
    }

    @Override
    public boolean existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
            UUID storeId, UUID denomId, String targetNumber,
            Collection<TransactionStatus> statuses, LocalDateTime since) {
        return jpaRepository.existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                storeId, denomId, targetNumber, statuses, since);
    }
}