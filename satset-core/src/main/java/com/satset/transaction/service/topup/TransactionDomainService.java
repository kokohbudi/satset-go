package com.satset.transaction.service.topup;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.logging.LogContext;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.dto.TransactionDTO;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.model.*;
import com.satset.transaction.client.ProviderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@LogContext("Topup")
public class TransactionDomainService {

        private final TransactionRepository transactionRepository;
        private final DenomRepository denomRepository;
        private final WalletGateway balanceService;
        private final ProviderPort providerService;
        private final RefNoGenerator refNoGenerator;

        public TransactionDomainService(TransactionRepository transactionRepository,
                        DenomRepository denomRepository,
                        WalletGateway balanceService,
                        ProviderPort providerService,
                        RefNoGenerator refNoGenerator) {
                this.transactionRepository = transactionRepository;
                this.denomRepository = denomRepository;
                this.balanceService = balanceService;
                this.providerService = providerService;
                this.refNoGenerator = refNoGenerator;
        }

        @Transactional
        public TransactionDTO createPurchase(UUID storeId, String walletId, UUID denomId, String targetNumber)
                        throws BusinessException {

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
                LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
                boolean isDuplicate = transactionRepository
                                .existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                                                storeId, denomId, targetNumber,
                                                List.of(TransactionStatus.PENDING,
                                                                TransactionStatus.PROCESSING,
                                                                TransactionStatus.SUCCESS),
                                                oneMinuteAgo);
                if (isDuplicate) {
                        log.warn("Duplicate transaction blocked: store={} denom={} target={}",
                                        storeId, denomId, targetNumber);
                        throw new BusinessException("DUPLICATE_TRANSACTION",
                                        "Harap tunggu 1 menit sebelum melakukan transaksi ke nomor yang sama.");
                }

                // 1. Create transaction (PENDING)
                Transactions transaction = new Transactions();
                transaction.setStoreId(storeId);
                transaction.setWalletId(walletId);
                transaction.setProductDenomId(denomId);
                transaction.setDenomName(denom.name());
                transaction.setProductName(denom.productName());
                transaction.setTargetNumber(targetNumber);
                transaction.setPrice(price);
                transaction.setAdminFee(adminFee);
                transaction.setTotal(total);
                transaction.setStatus(TransactionStatus.PENDING);
                transaction.setRefNo(refNoGenerator.next());
                transaction = transactionRepository.save(transaction);

                log.info("Transaction created: id={} store={} denom={} total={}",
                                transaction.getId(), storeId, denom.code(), total);

                // 2. Deduct balance
                balanceService.deductBalance(walletId, total,
                                transaction.getId(),
                                "Pembelian " + denom.name() + " ke " + targetNumber);

                // 3. Update status to PROCESSING
                transaction.setStatus(TransactionStatus.PROCESSING);
                transaction = transactionRepository.save(transaction);

                // 4. Send to provider
                ProviderResponse response = providerService.sendTransaction(
                                targetNumber, denom.code(), total, transaction.getRefNo());

                reconcileProviderResult(transaction, response, walletId, denom);

                return toDTO(transaction);
        }

        /**
         * Settle a PROCESSING transaction against a provider result. Shared by the
         * purchase flow and the reconcile poll.
         * <ul>
         *   <li>SUCCESS  → mark SUCCESS, snapshot ref/sn/cost/margin.
         *   <li>PENDING  → leave PROCESSING, keep providerRef, NO refund (poll settles later).
         *   <li>FAILED   → refund; on refund failure leave FAILED for manual Ops.
         * </ul>
         */
        public void reconcileProviderResult(Transactions transaction, ProviderResponse response, String walletId, DenomInfo denom) {
                if (response.status() == ProviderStatus.PENDING) {
                        if (response.referenceNumber() != null) {
                                transaction.setProviderRef(response.referenceNumber());
                        }
                        transactionRepository.save(transaction); // stays PROCESSING
                        log.info("Transaction PENDING: id={} ref={} — awaiting reconcile",
                                        transaction.getId(), response.referenceNumber());
                        return;
                }

                if (response.success()) {
                        transaction.setStatus(TransactionStatus.SUCCESS);
                        transaction.setProviderRef(response.referenceNumber());
                        transaction.setSerialNumber(response.serialNumber());
                        BigDecimal costPrice = response.cost() != null ? response.cost() : denom.basePrice();
                        transaction.setCostPrice(costPrice);
                        transaction.setMargin(costPrice != null
                                        ? transaction.getTotal().subtract(costPrice) : null);
                        transactionRepository.save(transaction);
                        log.info("Transaction SUCCESS: id={} ref={} sn={}",
                                        transaction.getId(), response.referenceNumber(), response.serialNumber());
                        return;
                }

                // FAILED → refund
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                try {
                        balanceService.refundBalance(walletId, transaction.getTotal(),
                                        transaction.getId(),
                                        "Refund " + denom.name() + " - " + response.message());
                        transaction.setStatus(TransactionStatus.REFUNDED);
                        transactionRepository.save(transaction);
                        log.warn("Transaction REFUNDED: id={} reason={}",
                                        transaction.getId(), response.message());
                } catch (Exception e) {
                        log.error("ALERT: Failed to refund transaction {} for wallet {}. Reason: {}",
                                        transaction.getId(), walletId, e.getMessage(), e);
                }
        }

        @Transactional(readOnly = true)
        public Page<TransactionDTO> getTransactionHistory(UUID storeId, Pageable pageable) {
                return transactionRepository.findByStoreId(storeId, pageable)
                                .map(this::toDTO);
        }

        private TransactionDTO toDTO(Transactions tx) {
                return new TransactionDTO(
                                tx.getId(),
                                tx.getRefNo(),
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
