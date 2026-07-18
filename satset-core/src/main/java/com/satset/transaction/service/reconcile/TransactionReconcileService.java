package com.satset.transaction.service.reconcile;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.TransactionDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Settles PROCESSING transactions left in-flight by a Digiflazz "Pending" response.
 * Re-POSTs /transaction with the same ref_id (idempotent, no re-charge) and applies
 * the current status via {@link TransactionDomainService#applyProviderResult}.
 *
 * <p>ponytail: batch cap per run so a backlog can't stampede Digiflazz's rate limit
 * (rc 85). Widen {@code supplier.reconcile.batch-size} if throughput needs it.
 */
@Slf4j
@Service
public class TransactionReconcileService {

    private final TransactionRepository txRepo;
    private final DenomRepository denomRepo;
    private final ProviderPort provider;
    private final TransactionDomainService txService;
    private final TransactionTemplate transactionTemplate;
    private final long staleAfterMs;
    private final int batchSize;

    public TransactionReconcileService(
            TransactionRepository txRepo,
            DenomRepository denomRepo,
            ProviderPort provider,
            TransactionDomainService txService,
            TransactionTemplate transactionTemplate,
            @Value("${supplier.reconcile.stale-after-ms:120000}") long staleAfterMs,
            @Value("${supplier.reconcile.batch-size:100}") int batchSize) {
        this.txRepo = txRepo;
        this.denomRepo = denomRepo;
        this.provider = provider;
        this.txService = txService;
        this.transactionTemplate = transactionTemplate;
        this.staleAfterMs = staleAfterMs;
        this.batchSize = batchSize;
    }

    /**
     * NOT {@code @Transactional}: each stale row is settled in its OWN transaction via
     * {@link #transactionTemplate} so a single failing row can't mark a shared transaction
     * rollback-only and poison every other settlement in the batch (a bare per-row try/catch
     * inside a single {@code @Transactional} method does NOT protect against this — Spring
     * still throws {@code UnexpectedRollbackException} at commit).
     */
    @Scheduled(fixedDelayString = "${supplier.reconcile.interval-ms:60000}")
    public void reconcileStalePending() {
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(staleAfterMs * 1_000_000);
        List<Transactions> stale = txRepo.findByStatusAndCreatedAtBefore(
                TransactionStatus.PROCESSING, cutoff,
                PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "createdAt")));
        if (stale.isEmpty()) return;

        log.info("Reconcile: {} stale PROCESSING tx", stale.size());
        for (Transactions stale1 : stale) {
            try {
                transactionTemplate.executeWithoutResult(status -> settleOne(stale1));
            } catch (Exception e) {
                log.error("Reconcile error tx {}: {}", stale1.getId(), e.getMessage(), e);
                // leave PROCESSING → retried next run; other rows in this batch are unaffected
            }
        }
    }

    private void settleOne(Transactions stale) {
        Transactions tx = txRepo.findById(stale.getId()).orElse(null);
        if (tx == null || tx.getStatus() != TransactionStatus.PROCESSING) return;
        DenomInfo denom = denomRepo.findDenomInfoById(tx.getProductDenomId()).orElse(null);
        if (denom == null) {
            log.warn("Reconcile skip: denom {} gone for tx {}", tx.getProductDenomId(), tx.getId());
            return;
        }
        ProviderResponse resp = provider.sendTransaction(
                tx.getTargetNumber(), denom.code(), tx.getTotal(), tx.getId().toString());
        txService.applyProviderResult(tx, resp, tx.getWalletId(), denom);
    }
}
