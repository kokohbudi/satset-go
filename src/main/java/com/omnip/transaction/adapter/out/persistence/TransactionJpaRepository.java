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

        @Query(value = "SELECT t FROM Transactions t " +
                        "JOIN FETCH t.productDenom pd " +
                        "JOIN FETCH pd.product " +
                        "WHERE t.store.id = :storeId " +
                        "ORDER BY t.createdAt DESC", countQuery = "SELECT COUNT(t) FROM Transactions t WHERE t.store.id = :storeId")
        Page<Transactions> findByStoreIdWithDetails(@Param("storeId") UUID storeId, Pageable pageable);

        @Query("SELECT t FROM Transactions t " +
                        "JOIN FETCH t.productDenom pd " +
                        "JOIN FETCH pd.product " +
                        "WHERE t.id = :id AND t.store.id = :storeId")
        Optional<Transactions> findByIdAndStoreIdWithDetails(@Param("id") UUID id, @Param("storeId") UUID storeId);
}
