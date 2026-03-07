package com.satset.transaction.adapter.out.persistence;

import com.satset.transaction.adapter.out.persistence.entity.StoreMutationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreMutationJpaRepository extends JpaRepository<StoreMutationJpaEntity, UUID> {

    Optional<StoreMutationJpaEntity> findTopByStoreIdOrderByCreatedAtDesc(UUID storeId);
}