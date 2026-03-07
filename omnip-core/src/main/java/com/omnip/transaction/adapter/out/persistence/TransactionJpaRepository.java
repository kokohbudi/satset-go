package com.omnip.transaction.adapter.out.persistence;

import com.omnip.transaction.adapter.out.persistence.entity.TransactionJpaEntity;
import com.omnip.transaction.domain.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionJpaEntity, UUID> {

    @Query("SELECT t FROM TransactionJpaEntity t WHERE t.storeId = :storeId ORDER BY t.createdAt DESC")
    Page<TransactionJpaEntity> findByStoreIdWithDetails(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("SELECT t FROM TransactionJpaEntity t WHERE t.id = :id AND t.storeId = :storeId")
    Optional<TransactionJpaEntity> findByIdAndStoreIdWithDetails(@Param("id") UUID id, @Param("storeId") UUID storeId);

    boolean existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
            UUID storeId, UUID denomId, String targetNumber,
            Collection<TransactionStatus> statuses,
            LocalDateTime since);
}