package com.satset.transaction.domain.service;

import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.domain.model.*;
import com.satset.transaction.domain.port.in.BalanceManagementUseCase;
import com.satset.transaction.domain.port.out.StoreMutationRepositoryPort;
import com.satset.transaction.domain.port.out.WalletAccountPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "wallet.client.enabled", havingValue = "false", matchIfMissing = true)
public class BalanceDomainService implements BalanceManagementUseCase {

    private final WalletAccountPort walletAccountPort;
    private final StoreMutationRepositoryPort storeMutationRepository;

    public BalanceDomainService(WalletAccountPort walletAccountPort,
            StoreMutationRepositoryPort storeMutationRepository) {
        this.walletAccountPort = walletAccountPort;
        this.storeMutationRepository = storeMutationRepository;
    }

    @Override
    @Transactional
    public MutationResult deductBalance(String walletId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description)
            throws InsufficientBalanceException {

        WalletAccount account = walletAccountPort.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo tidak mencukupi. Saldo: " + account.getBalance() + ", dibutuhkan: " + amount);
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        StoreMutations mutation = storeMutationRepository.save(
                buildMutation(walletId, amount, MutationType.DEBIT, newBalance, referenceType, referenceId, description));

        account.setBalance(newBalance);
        walletAccountPort.save(account);

        log.info("DEBIT wallet={} amount={} balanceAfter={} ref={}:{}",
                walletId, amount, newBalance, referenceType, referenceId);

        return new MutationResult(mutation.getId(), newBalance);
    }

    @Override
    @Transactional
    public MutationResult addBalance(String walletId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description) {

        WalletAccount account = walletAccountPort.findByWalletIdWithLock(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));

        BigDecimal newBalance = account.getBalance().add(amount);
        StoreMutations mutation = storeMutationRepository.save(
                buildMutation(walletId, amount, MutationType.CREDIT, newBalance, referenceType, referenceId, description));

        account.setBalance(newBalance);
        walletAccountPort.save(account);

        log.info("CREDIT wallet={} amount={} balanceAfter={} ref={}:{}",
                walletId, amount, newBalance, referenceType, referenceId);

        return new MutationResult(mutation.getId(), newBalance);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String walletId) {
        return walletAccountPort.findByWalletId(walletId)
                .map(WalletAccount::getBalance)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", walletId));
    }

    private StoreMutations buildMutation(String walletId, BigDecimal amount, MutationType type,
            BigDecimal balanceAfter, MutationReferenceType referenceType,
            UUID referenceId, String description) {
        StoreMutations m = new StoreMutations();
        m.setWalletId(walletId);
        m.setAmount(amount);
        m.setType(type);
        m.setBalanceAfter(balanceAfter);
        m.setReferenceType(referenceType);
        m.setReferenceId(referenceId);
        m.setDescription(description);
        return m;
    }
}
