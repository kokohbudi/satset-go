package com.omnip.transaction.adapter.out.persistence;

import com.omnip.transaction.domain.model.Transactions;
import com.omnip.transaction.domain.port.out.TransactionRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionJpaRepository extends JpaRepository<Transactions, UUID>, TransactionRepositoryPort {

        @Query("SELECT t FROM Transactions t WHERE t.storeId = :storeId ORDER BY t.createdAt DESC")
        Page<Transactions> findByStoreIdWithDetails(@Param("storeId") UUID storeId, Pageable pageable);

        @Query("SELECT t FROM Transactions t WHERE t.id = :id AND t.storeId = :storeId")
        Optional<Transactions> findByIdAndStoreIdWithDetails(@Param("id") UUID id, @Param("storeId") UUID storeId);

        boolean existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                        UUID storeId, UUID denomId, String targetNumber,
                        java.util.Collection<com.omnip.transaction.domain.model.TransactionStatus> statuses,
                        java.time.LocalDateTime since);
}
