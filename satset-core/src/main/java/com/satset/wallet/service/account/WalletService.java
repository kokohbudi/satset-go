package com.satset.wallet.service.account;

import com.satset.wallet.service.idgen.WalletIdGenerator;

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
        return applyCredit(walletId, amount, referenceId, MutationType.CREDIT, referenceType, description);
    }

    @Transactional
    public WalletMutationResult refund(String walletId, BigDecimal amount, UUID originalReferenceId, String description) {
        return applyCredit(walletId, amount, originalReferenceId, MutationType.REFUND,
                MutationReferenceType.REFUND, description);
    }

    /**
     * Idempotent balance increase shared by credit and refund: dedup by (referenceId, referenceType),
     * lock the account, add the amount, and record the mutation. Only mutationType/referenceType differ.
     */
    private WalletMutationResult applyCredit(String walletId, BigDecimal amount, UUID referenceId,
            MutationType mutationType, MutationReferenceType referenceType, String description) {

        log.info("{} {} to wallet {} with reference {}", mutationType, amount, walletId, referenceId);

        var existingMutation = walletMutationRepository.findByReferenceIdAndReferenceType(referenceId, referenceType);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate {} request for reference {}, returning existing result", mutationType, referenceId);
            WalletMutationEntity existing = existingMutation.get();
            return new WalletMutationResult(existing.getId(), existing.getBalanceAfter());
        }

        WalletAccountEntity account = walletAccountRepository.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        walletAccountRepository.save(account);

        WalletMutationEntity saved = walletMutationRepository.save(
                WalletMutationEntity.of(walletId, amount, mutationType, newBalance, referenceId, referenceType, description));

        log.info("{} successful: new balance for wallet {} is {}", mutationType, walletId, newBalance);
        return new WalletMutationResult(saved.getId(), newBalance);
    }

    public List<WalletMutationEntity> getMutations(String walletId) {
        return walletMutationRepository.findByWalletIdOrderByCreatedAtDesc(walletId);
    }

    /** All wallet accounts, newest first. Admin-facing (inject saldo page). */
    public List<WalletAccountEntity> listAccounts() {
        // ponytail: unpaged findAll; add pagination if wallet count grows past a screenful.
        return walletAccountRepository.findAll(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }
}
