package com.satset.transaction.domain.service;

import com.satset.catalog.adapter.out.persistence.DenomRepository;
import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.adapter.out.persistence.TransactionRepository;
import com.satset.transaction.adapter.out.wallet.WalletClientAdapter;
import com.satset.transaction.domain.model.*;
import com.satset.transaction.domain.port.out.ProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class TransactionDomainService {

        private final TransactionRepository transactionRepository;
        private final DenomRepository denomRepository;
        private final WalletClientAdapter balanceService;
        private final ProviderPort providerService;

        public TransactionDomainService(TransactionRepository transactionRepository,
                        DenomRepository denomRepository,
                        WalletClientAdapter balanceService,
                        ProviderPort providerService) {
                this.transactionRepository = transactionRepository;
                this.denomRepository = denomRepository;
                this.balanceService = balanceService;
                this.providerService = providerService;
        }

        @Transactional
        public TransactionSummary createPurchase(UUID storeId, String walletId, UUID denomId, String targetNumber)
                        throws InsufficientBalanceException {

                // Use DenomInfo (shared kernel) instead of ProductDenoms (catalog domain entity)
                DenomInfo denom = denomRepository.findDenomInfoById(denomId)
                                .orElseThrow(() -> new ResourceNotFoundException("ProductDenom", denomId));

                if (!denom.isAvailable()) {
                        throw new IllegalArgumentException("Product nominal is not active or has been deleted.");
                }

                BigDecimal price = denom.price();
                BigDecimal adminFee = denom.adminFee() != null ? denom.adminFee() : BigDecimal.ZERO;
                BigDecimal total = denom.total();

                // 0. Double submit protection (Idempotency check)
                java.time.LocalDateTime oneMinuteAgo = java.time.LocalDateTime.now().minusMinutes(1);
                boolean isDuplicate = transactionRepository
                                .existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                                                storeId, denomId, targetNumber,
                                                java.util.Arrays.asList(TransactionStatus.PENDING,
                                                                TransactionStatus.PROCESSING,
                                                                TransactionStatus.SUCCESS),
                                                oneMinuteAgo);
                if (isDuplicate) {
                        throw new IllegalArgumentException(
                                        "Harap tunggu 1 menit sebelum melakukan transaksi ke nomor yang sama.");
                }

                // 1. Create transaction (PENDING)
                Transactions transaction = new Transactions();
                transaction.setStoreId(storeId);
                transaction.setProductDenomId(denomId);
                transaction.setDenomName(denom.name());
                transaction.setProductName(denom.productName());
                transaction.setTargetNumber(targetNumber);
                transaction.setPrice(price);
                transaction.setAdminFee(adminFee);
                transaction.setTotal(total);
                transaction.setStatus(TransactionStatus.PENDING);
                transaction = transactionRepository.save(transaction);

                log.info("Transaction created: id={} store={} denom={} total={}",
                                transaction.getId(), storeId, denom.code(), total);

                // 2. Deduct balance
                balanceService.deductBalance(walletId, total,
                                MutationReferenceType.PURCHASE, transaction.getId(),
                                "Pembelian " + denom.name() + " ke " + targetNumber);

                // 3. Update status to PROCESSING
                transaction.setStatus(TransactionStatus.PROCESSING);
                transaction = transactionRepository.save(transaction);

                // 4. Send to provider
                ProviderResponse response = providerService.sendTransaction(
                                targetNumber, denom.code(), total);

                if (response.success()) {
                        // 5a. SUCCESS
                        transaction.setStatus(TransactionStatus.SUCCESS);
                        transaction.setProviderRef(response.referenceNumber());
                        transaction.setSerialNumber(response.serialNumber());
                        transaction = transactionRepository.save(transaction);

                        log.info("Transaction SUCCESS: id={} ref={} sn={}",
                                        transaction.getId(), response.referenceNumber(), response.serialNumber());
                } else {
                        // 5b. FAILED → refund
                        transaction.setStatus(TransactionStatus.FAILED);
                        transactionRepository.save(transaction);

                        try {
                                balanceService.addBalance(walletId, total,
                                                MutationReferenceType.REFUND, transaction.getId(),
                                                "Refund " + denom.name() + " - " + response.message());

                                transaction.setStatus(TransactionStatus.REFUNDED);
                                transaction = transactionRepository.save(transaction);

                                log.warn("Transaction REFUNDED: id={} reason={}",
                                                transaction.getId(), response.message());
                        } catch (Exception e) {
                                log.error("ALERT: Failed to refund transaction {} for wallet {}. Reason: {}",
                                        transaction.getId(), walletId, e.getMessage(), e);
                                // Leave status as FAILED so Ops team can retry manual refund
                        }
                }

                return toSummary(transaction);
        }

        @Transactional
        public void topUp(String walletId, BigDecimal amount, String description) {
                UUID topUpId = UUID.randomUUID();

                balanceService.addBalance(walletId, amount,
                                MutationReferenceType.TOP_UP, topUpId,
                                description != null ? description : "Manual top-up");

                log.info("Top-up completed: wallet={} amount={} topUpId={}", walletId, amount, topUpId);
        }

        @Transactional(readOnly = true)
        public TransactionSummary getTransaction(UUID id, UUID storeId) {
                Transactions tx = transactionRepository.findByIdAndStoreIdWithDetails(id, storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
                return toSummary(tx);
        }

        @Transactional(readOnly = true)
        public Page<TransactionSummary> getTransactionHistory(UUID storeId, Pageable pageable) {
                return transactionRepository.findByStoreIdWithDetails(storeId, pageable)
                                .map(this::toSummary);
        }

        private TransactionSummary toSummary(Transactions tx) {
                return new TransactionSummary(
                                tx.getId(),
                                tx.getStoreId(),
                                tx.getTargetNumber(),
                                tx.getDenomName(),
                                tx.getProductName(),
                                tx.getPrice(),
                                tx.getAdminFee(),
                                tx.getTotal(),
                                tx.getStatus(),
                                tx.getProviderRef(),
                                tx.getSerialNumber(),
                                tx.getCreatedAt());
        }
}
