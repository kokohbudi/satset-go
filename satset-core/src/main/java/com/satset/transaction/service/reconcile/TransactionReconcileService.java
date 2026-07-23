package com.satset.transaction.service.reconcile;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.logging.LogContext;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Core-side safety net for topups stuck in {@link TransactionStatus#PROCESSING} because
 * Digiflazz returned "Pending". Re-POSTs {@code /transaction} with the same ref_id
 * (idempotent, no re-charge — doubles as a status check) and settles via
 * {@link TransactionDomainService#reconcileProviderResult}. The primary settler is the
 * Fly webhook; this backstops late/missing webhooks (home IP is DF-whitelisted).
 *
 * <p>ponytail: batch cap per run so a backlog can't stampede DF's rate limit (rc 85).
 * Widen {@code topup.reconcile.batch-size} if throughput needs it.
 */
@Slf4j
@Service
@LogContext("Reconcile")
public class TransactionReconcileService {

    private final TransactionRepository txRepo;
    private final DenomRepository denomRepo;
    private final ProviderPort provider;
    private final TransactionDomainService txService;
    private final TransactionTemplate transactionTemplate;
    private final long staleAfterMs;
    private final int batchSize;
    private final long maxAgeMs;

    public TransactionReconcileService(
            TransactionRepository txRepo,
            DenomRepository denomRepo,
            ProviderPort provider,
            TransactionDomainService txService,
            TransactionTemplate transactionTemplate,
            @Value("${topup.reconcile.stale-after-ms:120000}") long staleAfterMs,
            @Value("${topup.reconcile.batch-size:100}") int batchSize,
            @Value("${topup.reconcile.max-age-ms:21600000}") long maxAgeMs) {
        this.txRepo = txRepo;
        this.denomRepo = denomRepo;
        this.provider = provider;
        this.txService = txService;
        this.transactionTemplate = transactionTemplate;
        this.staleAfterMs = staleAfterMs;
        this.batchSize = batchSize;
        this.maxAgeMs = maxAgeMs;
    }

    /**
     * NOT {@code @Transactional}: each row settles in its OWN transaction via
     * {@link #transactionTemplate}, so one failing row can't mark a shared transaction
     * rollback-only and poison every other settlement in the batch (a bare per-row
     * try/catch inside one {@code @Transactional} does NOT protect against this — Spring
     * still throws {@code UnexpectedRollbackException} at commit).
     */
    @Scheduled(fixedDelayString = "${topup.reconcile.interval-ms:60000}")
    public void reconcileStalePending() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleCutoff = now.minus(Duration.ofMillis(staleAfterMs)); // upper: must be older than this
        LocalDateTime maxCutoff = now.minus(Duration.ofMillis(maxAgeMs));        // lower: give-up past this

        // Alert on give-up rows, decoupled from the reconcile batch so they can't starve it.
        long stuck = txRepo.countByStatusAndCreatedAtBefore(TransactionStatus.PROCESSING, maxCutoff);
        if (stuck > 0) {
            log.error("ALERT: {} tx stuck PROCESSING > maxAge ({}h), need manual Ops",
                    stuck, maxAgeMs / 3_600_000);
        }

        List<Transactions> stale = txRepo.findByStatusAndCreatedAtBetween(
                TransactionStatus.PROCESSING, maxCutoff, staleCutoff,
                PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "createdAt")));
        if (stale.isEmpty()) return;

        log.info("Reconcile: {} stale PROCESSING tx", stale.size());
        for (Transactions row : stale) {
            try {
                transactionTemplate.executeWithoutResult(status -> settleOne(row));
            } catch (Exception e) {
                log.error("Reconcile error tx {}: {}", row.getId(), e.getMessage(), e);
                // leave PROCESSING → retried next run; other rows in this batch unaffected
            }
        }
    }

    private void settleOne(Transactions row) {
        Transactions tx = txRepo.findById(row.getId()).orElse(null);
        if (tx == null || tx.getStatus() != TransactionStatus.PROCESSING) return; // webhook already settled

        DenomInfo denom = denomRepo.findDenomInfoById(tx.getProductDenomId()).orElse(null);
        if (denom == null) {
            log.warn("Reconcile skip: denom {} gone for tx {}", tx.getProductDenomId(), tx.getId());
            return;
        }
        ProviderResponse resp = provider.sendTransaction(
                tx.getTargetNumber(), denom.code(), tx.getTotal(), refIdFor(tx));
        txService.reconcileProviderResult(tx, resp, tx.getWalletId(), denom);
    }

    /** Pre-ref_no rows have a null ref_no; fall back to the UUID so the re-POST still matches DF. */
    private static String refIdFor(Transactions tx) {
        return tx.getRefNo() != null ? tx.getRefNo() : tx.getId().toString();
    }
}
