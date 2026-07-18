package com.satset.transaction.service;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final long staleAfterMs;
    private final int batchSize;

    public TransactionReconcileService(
            TransactionRepository txRepo,
            DenomRepository denomRepo,
            ProviderPort provider,
            TransactionDomainService txService,
            @Value("${supplier.reconcile.stale-after-ms:120000}") long staleAfterMs,
            @Value("${supplier.reconcile.batch-size:100}") int batchSize) {
        this.txRepo = txRepo;
        this.denomRepo = denomRepo;
        this.provider = provider;
        this.txService = txService;
        this.staleAfterMs = staleAfterMs;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${supplier.reconcile.interval-ms:60000}")
    @Transactional
    public void reconcileStalePending() {
        LocalDateTime cutoff = LocalDateTime.now().minusNanos(staleAfterMs * 1_000_000);
        List<Transactions> stale = txRepo.findByStatusAndCreatedAtBefore(
                TransactionStatus.PROCESSING, cutoff, PageRequest.of(0, batchSize));
        if (stale.isEmpty()) return;

        log.info("Reconcile: {} stale PROCESSING tx", stale.size());
        for (Transactions tx : stale) {
            try {
                DenomInfo denom = denomRepo.findDenomInfoById(tx.getProductDenomId()).orElse(null);
                if (denom == null) {
                    log.warn("Reconcile skip: denom {} gone for tx {}", tx.getProductDenomId(), tx.getId());
                    continue;
                }
                ProviderResponse resp = provider.sendTransaction(
                        tx.getTargetNumber(), denom.code(), tx.getTotal(), tx.getId().toString());
                txService.applyProviderResult(tx, resp, tx.getWalletId(), denom);
            } catch (Exception e) {
                log.error("Reconcile error tx {}: {}", tx.getId(), e.getMessage(), e);
                // leave PROCESSING → retried next run
            }
        }
    }
}
