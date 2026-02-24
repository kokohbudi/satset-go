package com.omnip.transaction.domain.port.out;

import com.omnip.transaction.domain.model.Transactions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Note: save() is inherited from JpaRepository/CrudRepository.
 */
public interface TransactionRepositoryPort {

    Optional<Transactions> findByIdAndStoreIdWithDetails(UUID id, UUID storeId);

    Page<Transactions> findByStoreIdWithDetails(UUID storeId, Pageable pageable);
}
