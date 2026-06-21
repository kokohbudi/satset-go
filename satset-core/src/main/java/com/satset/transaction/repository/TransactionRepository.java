package com.satset.transaction.repository;

import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    Page<Transactions> findByStoreId(UUID storeId, Pageable pageable);

    Optional<Transactions> findByIdAndStoreId(UUID id, UUID storeId);

    boolean existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
            UUID storeId, UUID denomId, String targetNumber,
            Collection<TransactionStatus> statuses,
            LocalDateTime since);
}
