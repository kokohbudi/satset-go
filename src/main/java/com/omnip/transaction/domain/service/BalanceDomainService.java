package com.omnip.transaction.domain.service;

import com.omnip.transaction.domain.model.MutationResult;
import com.omnip.transaction.domain.model.MutationReferenceType;
import com.omnip.transaction.domain.model.MutationType;
import com.omnip.transaction.domain.model.StoreMutations;
import com.omnip.transaction.domain.model.WalletAccount;
import com.omnip.transaction.domain.port.in.BalanceManagementUseCase;
import com.omnip.transaction.domain.port.out.StoreMutationRepositoryPort;
import com.omnip.transaction.domain.port.out.WalletAccountPort;
import com.omnip.shared.exception.InsufficientBalanceException;
import com.omnip.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
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
    public MutationResult deductBalance(UUID storeId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description)
            throws InsufficientBalanceException {

        WalletAccount account = walletAccountPort.findByStoreIdWithLock(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", storeId));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo tidak mencukupi. Saldo: " + account.getBalance() + ", dibutuhkan: " + amount);
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        StoreMutations mutation = storeMutationRepository.save(
                buildMutation(storeId, amount, MutationType.DEBIT, newBalance, referenceType, referenceId, description));

        account.setBalance(newBalance);
        walletAccountPort.save(account);

        log.info("DEBIT store={} amount={} balanceAfter={} ref={}:{}",
                storeId, amount, newBalance, referenceType, referenceId);

        return new MutationResult(mutation.getId(), newBalance);
    }

    @Override
    @Transactional
    public MutationResult addBalance(UUID storeId, BigDecimal amount,
            MutationReferenceType referenceType, UUID referenceId, String description) {

        WalletAccount account = walletAccountPort.findByStoreIdWithLock(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", storeId));

        BigDecimal newBalance = account.getBalance().add(amount);
        StoreMutations mutation = storeMutationRepository.save(
                buildMutation(storeId, amount, MutationType.CREDIT, newBalance, referenceType, referenceId, description));

        account.setBalance(newBalance);
        walletAccountPort.save(account);

        log.info("CREDIT store={} amount={} balanceAfter={} ref={}:{}",
                storeId, amount, newBalance, referenceType, referenceId);

        return new MutationResult(mutation.getId(), newBalance);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID storeId) {
        return walletAccountPort.findByStoreId(storeId)
                .map(WalletAccount::getBalance)
                .orElseThrow(() -> new ResourceNotFoundException("WalletAccount", storeId));
    }

    private StoreMutations buildMutation(UUID storeId, BigDecimal amount, MutationType type,
            BigDecimal balanceAfter, MutationReferenceType referenceType,
            UUID referenceId, String description) {
        StoreMutations m = new StoreMutations();
        m.setStoreId(storeId);
        m.setAmount(amount);
        m.setType(type);
        m.setBalanceAfter(balanceAfter);
        m.setReferenceType(referenceType);
        m.setReferenceId(referenceId);
        m.setDescription(description);
        return m;
    }
}
