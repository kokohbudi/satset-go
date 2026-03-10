package com.satset.wallet.domain;

import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.MutationType;
import com.satset.wallet.domain.model.WalletAccount;
import com.satset.wallet.domain.model.WalletMutation;
import com.satset.wallet.domain.port.in.WalletUseCase;
import com.satset.wallet.domain.port.out.WalletAccountPort;
import com.satset.wallet.domain.port.out.WalletMutationPort;
import com.satset.wallet.domain.service.WalletIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class WalletDomainService implements WalletUseCase {

    private static final Logger log = LoggerFactory.getLogger(WalletDomainService.class);

    private final WalletAccountPort walletAccountPort;
    private final WalletMutationPort walletMutationPort;
    private final WalletIdGenerator walletIdGenerator;

    public WalletDomainService(WalletAccountPort walletAccountPort,
                               WalletMutationPort walletMutationPort,
                               WalletIdGenerator walletIdGenerator) {
        this.walletAccountPort = walletAccountPort;
        this.walletMutationPort = walletMutationPort;
        this.walletIdGenerator = walletIdGenerator;
    }

    @Override
    @Transactional
    public WalletAccount createWallet() {
        String walletId = walletIdGenerator.generate();
        log.info("Creating wallet with id {}", walletId);

        WalletAccount newAccount = WalletAccount.newAccount(walletId);
        WalletAccount saved = walletAccountPort.save(newAccount);

        log.info("Wallet created successfully: {}", walletId);
        return saved;
    }

    @Override
    public BigDecimal getBalance(String walletId) {
        return walletAccountPort.findByWalletId(walletId)
                .map(WalletAccount::balance)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));
    }

    @Override
    @Transactional
    public WalletMutationResult debit(String walletId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description) {

        log.info("Debiting {} from wallet {} with reference {}", amount, walletId, referenceId);

        var existingMutation = walletMutationPort.findByReferenceIdAndReferenceType(referenceId, referenceType);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate debit request for reference {}, returning existing result", referenceId);
            WalletMutation existing = existingMutation.get();
            return new WalletMutationResult(existing.id(), existing.balanceAfter());
        }

        WalletAccount account = walletAccountPort.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));

        BigDecimal currentBalance = account.balance();
        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, currentBalance);
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        walletAccountPort.save(account.withBalance(newBalance));

        WalletMutation saved = walletMutationPort.save(
                WalletMutation.of(walletId, amount, MutationType.DEBIT, newBalance, referenceId, referenceType, description));

        log.info("Debit successful: new balance for wallet {} is {}", walletId, newBalance);
        return new WalletMutationResult(saved.id(), newBalance);
    }

    @Override
    @Transactional
    public WalletMutationResult credit(String walletId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description) {

        log.info("Crediting {} to wallet {} with reference {}", amount, walletId, referenceId);

        var existingMutation = walletMutationPort.findByReferenceIdAndReferenceType(referenceId, referenceType);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate credit request for reference {}, returning existing result", referenceId);
            WalletMutation existing = existingMutation.get();
            return new WalletMutationResult(existing.id(), existing.balanceAfter());
        }

        WalletAccount account = walletAccountPort.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));

        BigDecimal newBalance = account.balance().add(amount);
        walletAccountPort.save(account.withBalance(newBalance));

        WalletMutation saved = walletMutationPort.save(
                WalletMutation.of(walletId, amount, MutationType.CREDIT, newBalance, referenceId, referenceType, description));

        log.info("Credit successful: new balance for wallet {} is {}", walletId, newBalance);
        return new WalletMutationResult(saved.id(), newBalance);
    }

    @Override
    @Transactional
    public WalletMutationResult refund(String walletId, BigDecimal amount, UUID originalReferenceId, String description) {

        log.info("Refunding {} to wallet {} for original reference {}", amount, walletId, originalReferenceId);

        var existingMutation = walletMutationPort.findByReferenceIdAndReferenceType(
                originalReferenceId, MutationReferenceType.REFUND);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate refund request for reference {}, returning existing result", originalReferenceId);
            WalletMutation existing = existingMutation.get();
            return new WalletMutationResult(existing.id(), existing.balanceAfter());
        }

        WalletAccount account = walletAccountPort.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));

        BigDecimal newBalance = account.balance().add(amount);
        walletAccountPort.save(account.withBalance(newBalance));

        WalletMutation saved = walletMutationPort.save(
                WalletMutation.of(walletId, amount, MutationType.REFUND, newBalance,
                        originalReferenceId, MutationReferenceType.REFUND, description));

        log.info("Refund successful: new balance for wallet {} is {}", walletId, newBalance);
        return new WalletMutationResult(saved.id(), newBalance);
    }

    @Override
    public List<WalletMutation> getMutations(String walletId) {
        return walletMutationPort.findByWalletIdOrderByCreatedAtDesc(walletId);
    }
}
