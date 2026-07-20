package com.satset.webhook.service;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.logging.LogContext;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.TransactionDomainService;
import com.satset.webhook.dto.DigiflazzWebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles a verified Digiflazz webhook payload: looks up the transaction by
 * ref_id, guards against replay (WH-4 backstop — the real guard lives in
 * {@link TransactionDomainService#reconcileProviderResult}), and settles it.
 */
@Slf4j
@Service
@LogContext("Webhook")
public class DigiflazzWebhookService {

    public enum HandleResult { SETTLED, REPLAY_IGNORED }

    private final TransactionRepository transactionRepository;
    private final DenomRepository denomRepository;
    private final TransactionDomainService transactionDomainService;

    public DigiflazzWebhookService(TransactionRepository transactionRepository,
                                    DenomRepository denomRepository,
                                    TransactionDomainService transactionDomainService) {
        this.transactionRepository = transactionRepository;
        this.denomRepository = denomRepository;
        this.transactionDomainService = transactionDomainService;
    }

    @Transactional
    public HandleResult handle(DigiflazzWebhookPayload.Data data) {
        Transactions transaction = transactionRepository.findByRefNo(data.refId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", data.refId()));

        if (isTerminal(transaction.getStatus())) {
            log.info("Webhook replay ignored: refId={} status={}", data.refId(), transaction.getStatus());
            return HandleResult.REPLAY_IGNORED;
        }

        DenomInfo denom = denomRepository.findDenomInfoById(transaction.getProductDenomId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductDenom", transaction.getProductDenomId()));

        ProviderResponse response = data.toProviderResponse();
        transactionDomainService.reconcileProviderResult(transaction, response, transaction.getWalletId(), denom);
        return HandleResult.SETTLED;
    }

    private static boolean isTerminal(TransactionStatus status) {
        return status == TransactionStatus.SUCCESS
                || status == TransactionStatus.FAILED
                || status == TransactionStatus.REFUNDED;
    }
}
