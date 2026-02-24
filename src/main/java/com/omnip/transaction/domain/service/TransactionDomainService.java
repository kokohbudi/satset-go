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
import com.omnip.shared.exception.InsufficientBalanceException;
import com.omnip.shared.exception.ResourceNotFoundException;
import com.omnip.catalog.adapter.out.persistence.DenomJpaRepository;
import com.omnip.onboarding.adapter.out.persistence.StoreJpaRepository;
import com.omnip.transaction.adapter.out.persistence.TransactionJpaRepository;
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

        private final TransactionJpaRepository transactionRepository;
        private final StoreJpaRepository storeRepository;
        private final DenomJpaRepository productDenomRepository;
        private final BalanceDomainService balanceService;
        private final ProviderPort providerService;

        public TransactionDomainService(TransactionJpaRepository transactionRepository,
                        StoreJpaRepository storeRepository,
                        DenomJpaRepository productDenomRepository,
                        BalanceDomainService balanceService,
                        ProviderPort providerService) {
                this.transactionRepository = transactionRepository;
                this.storeRepository = storeRepository;
                this.productDenomRepository = productDenomRepository;
                this.balanceService = balanceService;
                this.providerService = providerService;
        }

        @Override
        public Transactions createPurchase(UUID storeId, UUID denomId, String targetNumber)
                        throws InsufficientBalanceException {

                Stores store = storeRepository.findById(storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));

                ProductDenoms denom = productDenomRepository.findById(denomId)
                                .orElseThrow(() -> new ResourceNotFoundException("ProductDenom", denomId));

                BigDecimal price = denom.getPrice();
                BigDecimal adminFee = denom.getAdminFee() != null ? denom.getAdminFee() : BigDecimal.ZERO;
                BigDecimal total = price.add(adminFee);

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

                        balanceService.addBalance(storeId, total,
                                        MutationReferenceType.REFUND, transaction.getId(),
                                        "Refund " + denom.getName() + " - " + response.message());

                        transaction.setStatus(TransactionStatus.REFUNDED);
                        transaction = transactionRepository.save(transaction);

                        log.warn("Transaction REFUNDED: id={} reason={}",
                                        transaction.getId(), response.message());
                }

                return transaction;
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
        public Transactions getTransaction(UUID id, UUID storeId) {
                return transactionRepository.findByIdAndStoreIdWithDetails(id, storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        }

        @Override
        @Transactional(readOnly = true)
        public Page<Transactions> getTransactionHistory(UUID storeId, Pageable pageable) {
                storeRepository.findById(storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
                return transactionRepository.findByStoreIdWithDetails(storeId, pageable);
        }
}
