package com.satset.transaction.adapter.out.persistence;

import com.satset.transaction.domain.model.TransactionStatus;
import com.satset.transaction.domain.model.Transactions;
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

/**
 * Transaction persistence. Spring Data provides the implementation.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transactions, UUID> {

    @Query("SELECT t FROM Transactions t WHERE t.storeId = :storeId ORDER BY t.createdAt DESC")
    Page<Transactions> findByStoreIdWithDetails(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("SELECT t FROM Transactions t WHERE t.id = :id AND t.storeId = :storeId")
    Optional<Transactions> findByIdAndStoreIdWithDetails(@Param("id") UUID id, @Param("storeId") UUID storeId);

    boolean existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
            UUID storeId, UUID denomId, String targetNumber,
            Collection<TransactionStatus> statuses,
            LocalDateTime since);
}
