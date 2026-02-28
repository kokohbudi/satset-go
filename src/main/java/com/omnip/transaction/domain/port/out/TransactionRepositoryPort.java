package com.omnip.transaction.domain.port.out;

import com.omnip.transaction.domain.model.Transactions;
import com.omnip.transaction.domain.model.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepositoryPort {

    Transactions save(Transactions transaction);

    Optional<Transactions> findByIdAndStoreIdWithDetails(UUID id, UUID storeId);

    Page<Transactions> findByStoreIdWithDetails(UUID storeId, Pageable pageable);

    boolean existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
            UUID storeId, UUID denomId, String targetNumber,
            Collection<TransactionStatus> statuses,
            LocalDateTime since);
}
