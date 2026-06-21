package com.satset.wallet.service;

import com.satset.wallet.model.MutationReferenceType;
import com.satset.wallet.model.MutationType;
import com.satset.wallet.model.WalletAccountEntity;
import com.satset.wallet.model.WalletMutationEntity;
import com.satset.wallet.repository.WalletAccountRepository;
import com.satset.wallet.repository.WalletMutationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletAccountRepository walletAccountRepository;
    private final WalletMutationRepository walletMutationRepository;
    private final WalletIdGenerator walletIdGenerator;

    public WalletService(WalletAccountRepository walletAccountRepository,
                         WalletMutationRepository walletMutationRepository,
                         WalletIdGenerator walletIdGenerator) {
        this.walletAccountRepository = walletAccountRepository;
        this.walletMutationRepository = walletMutationRepository;
        this.walletIdGenerator = walletIdGenerator;
    }

    @Transactional
    public WalletAccountEntity createWallet() {
        String walletId = walletIdGenerator.generate();
        log.info("Creating wallet with id {}", walletId);

        WalletAccountEntity saved = walletAccountRepository.save(WalletAccountEntity.newAccount(walletId));

        log.info("Wallet created successfully: {}", walletId);
        return saved;
    }

    public BigDecimal getBalance(String walletId) {
        return walletAccountRepository.findById(walletId)
                .map(WalletAccountEntity::getBalance)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));
    }

    @Transactional
    public WalletMutationResult debit(String walletId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description) {

        log.info("Debiting {} from wallet {} with reference {}", amount, walletId, referenceId);

        var existingMutation = walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, referenceType);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate debit request for reference {}, returning existing result", referenceId);
            WalletMutationEntity existing = existingMutation.get();
            return new WalletMutationResult(existing.getId(), existing.getBalanceAfter());
        }

        WalletAccountEntity account = walletAccountRepository.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));

        BigDecimal currentBalance = account.getBalance();
        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, currentBalance);
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        account.setBalance(newBalance);
        walletAccountRepository.save(account);

        WalletMutationEntity saved = walletMutationRepository.save(
                WalletMutationEntity.of(walletId, amount, MutationType.DEBIT, newBalance, referenceId, referenceType, description));

        log.info("Debit successful: new balance for wallet {} is {}", walletId, newBalance);
        return new WalletMutationResult(saved.getId(), newBalance);
    }

    @Transactional
    public WalletMutationResult credit(String walletId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description) {

        log.info("Crediting {} to wallet {} with reference {}", amount, walletId, referenceId);

        var existingMutation = walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, referenceType);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate credit request for reference {}, returning existing result", referenceId);
            WalletMutationEntity existing = existingMutation.get();
            return new WalletMutationResult(existing.getId(), existing.getBalanceAfter());
        }

        WalletAccountEntity account = walletAccountRepository.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        walletAccountRepository.save(account);

        WalletMutationEntity saved = walletMutationRepository.save(
                WalletMutationEntity.of(walletId, amount, MutationType.CREDIT, newBalance, referenceId, referenceType, description));

        log.info("Credit successful: new balance for wallet {} is {}", walletId, newBalance);
        return new WalletMutationResult(saved.getId(), newBalance);
    }

    @Transactional
    public WalletMutationResult refund(String walletId, BigDecimal amount, UUID originalReferenceId, String description) {

        log.info("Refunding {} to wallet {} for original reference {}", amount, walletId, originalReferenceId);

        var existingMutation = walletMutationRepository.findByReferenceIdAndReferenceType(
                originalReferenceId, MutationReferenceType.REFUND);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate refund request for reference {}, returning existing result", originalReferenceId);
            WalletMutationEntity existing = existingMutation.get();
            return new WalletMutationResult(existing.getId(), existing.getBalanceAfter());
        }

        WalletAccountEntity account = walletAccountRepository.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        walletAccountRepository.save(account);

        WalletMutationEntity saved = walletMutationRepository.save(
                WalletMutationEntity.of(walletId, amount, MutationType.REFUND, newBalance,
                        originalReferenceId, MutationReferenceType.REFUND, description));

        log.info("Refund successful: new balance for wallet {} is {}", walletId, newBalance);
        return new WalletMutationResult(saved.getId(), newBalance);
    }

    public List<WalletMutationEntity> getMutations(String walletId) {
        return walletMutationRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
    }
}
