package com.satset.wallet.domain;

import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.MutationType;
import com.satset.wallet.domain.model.WalletAccount;
import com.satset.wallet.domain.model.WalletMutation;
import com.satset.wallet.domain.port.in.WalletUseCase;
import com.satset.wallet.domain.port.out.WalletAccountPort;
import com.satset.wallet.domain.port.out.WalletMutationPort;
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

    public WalletDomainService(WalletAccountPort walletAccountPort, WalletMutationPort walletMutationPort) {
        this.walletAccountPort = walletAccountPort;
        this.walletMutationPort = walletMutationPort;
    }

    @Override
    public BigDecimal getBalance(UUID storeId) {
        return walletAccountPort.findByStoreId(storeId)
                .map(WalletAccount::balance)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", storeId));
    }

    @Override
    @Transactional
    public WalletMutationResult debit(UUID storeId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description) {

        log.info("Debiting {} from store {} with reference {}", amount, storeId, referenceId);

        var existingMutation = walletMutationPort.findByReferenceIdAndReferenceType(referenceId, referenceType);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate debit request for reference {}, returning existing result", referenceId);
            WalletMutation existing = existingMutation.get();
            return new WalletMutationResult(existing.id(), existing.balanceAfter());
        }

        WalletAccount account = walletAccountPort.findByStoreIdWithLock(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", storeId));

        BigDecimal currentBalance = account.balance();
        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, currentBalance);
        }

        BigDecimal newBalance = currentBalance.subtract(amount);
        walletAccountPort.save(account.withBalance(newBalance));

        WalletMutation saved = walletMutationPort.save(
                WalletMutation.of(storeId, amount, MutationType.DEBIT, newBalance, referenceId, referenceType, description));

        log.info("Debit successful: new balance for store {} is {}", storeId, newBalance);
        return new WalletMutationResult(saved.id(), newBalance);
    }

    @Override
    @Transactional
    public WalletMutationResult credit(UUID storeId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description) {

        log.info("Crediting {} to store {} with reference {}", amount, storeId, referenceId);

        var existingMutation = walletMutationPort.findByReferenceIdAndReferenceType(referenceId, referenceType);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate credit request for reference {}, returning existing result", referenceId);
            WalletMutation existing = existingMutation.get();
            return new WalletMutationResult(existing.id(), existing.balanceAfter());
        }

        WalletAccount account = walletAccountPort.findByStoreIdWithLock(storeId)
                .orElseGet(() -> walletAccountPort.save(WalletAccount.newAccount(storeId)));

        BigDecimal newBalance = account.balance().add(amount);
        walletAccountPort.save(account.withBalance(newBalance));

        WalletMutation saved = walletMutationPort.save(
                WalletMutation.of(storeId, amount, MutationType.CREDIT, newBalance, referenceId, referenceType, description));

        log.info("Credit successful: new balance for store {} is {}", storeId, newBalance);
        return new WalletMutationResult(saved.id(), newBalance);
    }

    @Override
    @Transactional
    public WalletMutationResult refund(UUID storeId, BigDecimal amount, UUID originalReferenceId, String description) {

        log.info("Refunding {} to store {} for original reference {}", amount, storeId, originalReferenceId);

        var existingMutation = walletMutationPort.findByReferenceIdAndReferenceType(
                originalReferenceId, MutationReferenceType.REFUND);
        if (existingMutation.isPresent()) {
            log.warn("Duplicate refund request for reference {}, returning existing result", originalReferenceId);
            WalletMutation existing = existingMutation.get();
            return new WalletMutationResult(existing.id(), existing.balanceAfter());
        }

        WalletAccount account = walletAccountPort.findByStoreIdWithLock(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", storeId));

        BigDecimal newBalance = account.balance().add(amount);
        walletAccountPort.save(account.withBalance(newBalance));

        WalletMutation saved = walletMutationPort.save(
                WalletMutation.of(storeId, amount, MutationType.REFUND, newBalance,
                        originalReferenceId, MutationReferenceType.REFUND, description));

        log.info("Refund successful: new balance for store {} is {}", storeId, newBalance);
        return new WalletMutationResult(saved.id(), newBalance);
    }

    @Override
    public List<WalletMutation> getMutations(UUID storeId) {
        return walletMutationPort.findByStoreIdOrderByCreatedAtDesc(storeId);
    }
}
