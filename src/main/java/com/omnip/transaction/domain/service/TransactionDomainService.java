package com.omnip.transaction.domain.service;

import com.omnip.transaction.domain.model.ProviderResponse;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.transaction.domain.port.in.PurchaseUseCase;
import com.omnip.transaction.domain.port.in.TopUpUseCase;
import com.omnip.transaction.domain.port.in.TransactionQueryUseCase;
import com.omnip.transaction.domain.port.out.ProviderPort;
import com.omnip.transaction.domain.model.Transactions;
import com.omnip.transaction.domain.model.MutationReferenceType;
import com.omnip.transaction.domain.model.TransactionStatus;
import com.omnip.transaction.domain.model.TransactionSummary;
import com.omnip.shared.exception.InsufficientBalanceException;
import com.omnip.shared.exception.ResourceNotFoundException;
import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import com.omnip.transaction.domain.port.out.StoreBalancePort;
import com.omnip.transaction.domain.port.out.TransactionRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class TransactionDomainService implements PurchaseUseCase, TopUpUseCase, TransactionQueryUseCase {

        private final TransactionRepositoryPort transactionRepository;
        private final StoreBalancePort storeRepository;
        private final DenomRepositoryPort productDenomRepository;
        private final BalanceDomainService balanceService;
        private final ProviderPort providerService;

        public TransactionDomainService(TransactionRepositoryPort transactionRepository,
                        StoreBalancePort storeRepository,
                        DenomRepositoryPort productDenomRepository,
                        BalanceDomainService balanceService,
                        ProviderPort providerService) {
                this.transactionRepository = transactionRepository;
                this.storeRepository = storeRepository;
                this.productDenomRepository = productDenomRepository;
                this.balanceService = balanceService;
                this.providerService = providerService;
        }

        @Override
        @Transactional
        public TransactionSummary createPurchase(UUID storeId, UUID denomId, String targetNumber)
                        throws InsufficientBalanceException {

                Stores store = storeRepository.findById(storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));

                ProductDenoms denom = productDenomRepository.findById(denomId)
                                .orElseThrow(() -> new ResourceNotFoundException("ProductDenom", denomId));

                if (!denom.isActive() || denom.isDeleted()) {
                        throw new IllegalArgumentException("Product nominal is not active or has been deleted.");
                }

                BigDecimal price = denom.getPrice();
                BigDecimal adminFee = denom.getAdminFee() != null ? denom.getAdminFee() : BigDecimal.ZERO;
                BigDecimal total = price.add(adminFee);

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
                transaction.setStore(store);
                transaction.setProductDenom(denom);
                transaction.setTargetNumber(targetNumber);
                transaction.setPrice(price);
                transaction.setAdminFee(adminFee);
                transaction.setTotal(total);
                transaction.setStatus(TransactionStatus.PENDING);
                transaction = transactionRepository.save(transaction);

                log.info("Transaction created: id={} store={} denom={} total={}",
                                transaction.getId(), storeId, denom.getCode(), total);

                // 2. Deduct balance
                balanceService.deductBalance(storeId, total,
                                MutationReferenceType.PURCHASE, transaction.getId(),
                                "Pembelian " + denom.getName() + " ke " + targetNumber);

                // 3. Update status to PROCESSING
                transaction.setStatus(TransactionStatus.PROCESSING);
                transaction = transactionRepository.save(transaction);

                // 4. Send to provider
                ProviderResponse response = providerService.sendTransaction(
                                targetNumber, denom.getCode(), total);

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
                                balanceService.addBalance(storeId, total,
                                                MutationReferenceType.REFUND, transaction.getId(),
                                                "Refund " + denom.getName() + " - " + response.message());

                                transaction.setStatus(TransactionStatus.REFUNDED);
                                transaction = transactionRepository.save(transaction);

                                log.warn("Transaction REFUNDED: id={} reason={}",
                                                transaction.getId(), response.message());
                        } catch (Exception e) {
                                log.error("ALERT: Failed to refund transaction {} for store {}. Reason: {}",
                                                transaction.getId(), storeId, e.getMessage(), e);
                                // Leave status as FAILED so Ops team can retry manual refund
                        }
                }

                return toSummary(transaction);
        }

        @Override
        @Transactional
        public void topUp(UUID storeId, BigDecimal amount, String description) {
                storeRepository.findById(storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));

                UUID topUpId = UUID.randomUUID();

                balanceService.addBalance(storeId, amount,
                                MutationReferenceType.TOP_UP, topUpId,
                                description != null ? description : "Manual top-up");

                log.info("Top-up completed: store={} amount={} topUpId={}", storeId, amount, topUpId);
        }

        @Override
        @Transactional(readOnly = true)
        public TransactionSummary getTransaction(UUID id, UUID storeId) {
                Transactions tx = transactionRepository.findByIdAndStoreIdWithDetails(id, storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
                return toSummary(tx);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<TransactionSummary> getTransactionHistory(UUID storeId, Pageable pageable) {
                storeRepository.findById(storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
                return transactionRepository.findByStoreIdWithDetails(storeId, pageable)
                                .map(this::toSummary);
        }

        private TransactionSummary toSummary(Transactions tx) {
                var denom = tx.getProductDenom();
                return new TransactionSummary(
                                tx.getId(),
                                tx.getStore().getId(),
                                tx.getTargetNumber(),
                                denom.getName(),
                                denom.getProduct() != null ? denom.getProduct().getName() : null,
                                tx.getPrice(),
                                tx.getAdminFee(),
                                tx.getTotal(),
                                tx.getStatus(),
                                tx.getProviderRef(),
                                tx.getSerialNumber(),
                                tx.getCreatedAt());
        }
}
