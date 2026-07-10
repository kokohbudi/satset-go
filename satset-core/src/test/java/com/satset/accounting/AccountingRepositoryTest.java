package com.satset.accounting;

import com.satset.accounting.dto.PnlRow;
import com.satset.accounting.dto.PnlSummary;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository-level test for the P&amp;L aggregate queries (Slice 1a).
 * Mirrors {@code PurchaseFlowIntegrationTest}'s {@code @SpringBootTest} setup, but
 * autowires the real {@link TransactionRepository} (not a {@code @MockitoBean})
 * so the JPQL aggregates run against the actual dev Postgres instance. Wrapped in
 * {@code @Transactional} so seeded rows are rolled back after each test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
class AccountingRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

    // transactions.store_id / product_denom_id carry FK constraints to stores /
    // product_denoms. Random UUIDs (as in the task brief's literal save() helper) violate
    // those FKs against the real dev Postgres this test runs against (no mocked repository
    // here, unlike PurchaseFlowIntegrationTest). Reuse one already-seeded row of each — the
    // aggregate queries under test don't care which store/denom a row belongs to.
    private UUID seededStoreId;
    private UUID seededDenomId;

    @BeforeEach
    void resolveSeededForeignKeys() {
        seededStoreId = (UUID) entityManager.createNativeQuery("SELECT id FROM stores LIMIT 1")
                .getSingleResult();
        seededDenomId = (UUID) entityManager.createNativeQuery("SELECT id FROM product_denoms LIMIT 1")
                .getSingleResult();
    }

    @Test
    void summarizePnl_sumsOnlySuccessRowsInRange() {
        LocalDateTime now = LocalDateTime.now();
        // in-range SUCCESS: revenue 6500, cost 6000, margin 500
        save(6500, 6000L, 500L, TransactionStatus.SUCCESS, now.minusHours(1), "IM3");
        // in-range SUCCESS second product
        save(11000, 10000L, 1000L, TransactionStatus.SUCCESS, now.minusHours(2), "Telkomsel");
        // FAILED -> excluded
        save(5000, null, null, TransactionStatus.FAILED, now.minusHours(1), "IM3");
        // out of range -> excluded
        save(9999, 9000L, 999L, TransactionStatus.SUCCESS, now.minusDays(5), "IM3");

        LocalDateTime from = now.minusDays(1);
        LocalDateTime to = now.plusMinutes(1);

        PnlSummary s = transactionRepository.summarizePnl(from, to);
        assertThat(s.revenue()).isEqualByComparingTo("17500");
        assertThat(s.cogs()).isEqualByComparingTo("16000");
        assertThat(s.margin()).isEqualByComparingTo("1500");
        assertThat(s.count()).isEqualTo(2);

        List<PnlRow> rows = transactionRepository.summarizePnlByProduct(from, to);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).label()).isEqualTo("Telkomsel"); // ordered by margin desc
        assertThat(rows.get(0).margin()).isEqualByComparingTo("1000");
    }

    private void save(long total, Long cost, Long margin, TransactionStatus status,
                      LocalDateTime createdAt, String product) {
        Transactions t = new Transactions();
        t.setStoreId(seededStoreId);
        t.setProductDenomId(seededDenomId);
        t.setProductName(product);
        t.setDenomName(product + " denom");
        t.setTargetNumber("081200000000");
        t.setPrice(new BigDecimal(total));
        t.setAdminFee(BigDecimal.ZERO);
        t.setTotal(new BigDecimal(total));
        t.setCostPrice(cost != null ? new BigDecimal(cost) : null);
        t.setMargin(margin != null ? new BigDecimal(margin) : null);
        t.setStatus(status);
        Transactions saved = transactionRepository.saveAndFlush(t);

        // createdAt is @CreatedDate with updatable = false: Spring Data auditing sets it
        // on first persist and Hibernate silently ignores later updates to that column
        // (it's excluded from the UPDATE statement). Setting it via the entity setter and
        // save()/saveAndFlush() would therefore NOT persist the override. Force it with a
        // native UPDATE instead, then flush + clear the persistence context so the
        // aggregate JPQL queries re-read the overridden value from the DB rather than
        // returning the stale in-memory entity from the first-level cache.
        entityManager.createNativeQuery("UPDATE transactions SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, saved.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }
}
