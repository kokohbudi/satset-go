package com.satset.transaction.repository;

import com.satset.accounting.dto.PnlRow;
import com.satset.accounting.dto.PnlSummary;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Transaction persistence. Spring Data provides the implementation.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transactions, UUID> {

    Page<Transactions> findByStoreId(UUID storeId, Pageable pageable);

    // ref_no == Digiflazz's ref_id — lookup for webhook reconcile.
    Optional<Transactions> findByRefNo(String refNo);

    boolean existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
            UUID storeId, UUID denomId, String targetNumber,
            Collection<TransactionStatus> statuses,
            LocalDateTime since);

    // ponytail: raw aggregate over transactions; add a daily rollup table only if this query gets slow at volume
    @Query("""
            SELECT new com.satset.accounting.dto.PnlSummary(
                COALESCE(SUM(t.total), 0), COALESCE(SUM(t.costPrice), 0),
                COALESCE(SUM(t.margin), 0), COUNT(t))
            FROM Transactions t
            WHERE t.status = com.satset.transaction.model.TransactionStatus.SUCCESS
              AND t.createdAt >= :from AND t.createdAt < :to
            """)
    PnlSummary summarizePnl(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
            SELECT new com.satset.accounting.dto.PnlRow(
                t.productName, COALESCE(SUM(t.total), 0), COALESCE(SUM(t.costPrice), 0),
                COALESCE(SUM(t.margin), 0), COUNT(t))
            FROM Transactions t
            WHERE t.status = com.satset.transaction.model.TransactionStatus.SUCCESS
              AND t.createdAt >= :from AND t.createdAt < :to
            GROUP BY t.productName
            ORDER BY COALESCE(SUM(t.margin), 0) DESC
            """)
    List<PnlRow> summarizePnlByProduct(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
