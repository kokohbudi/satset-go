package com.omnip.transaction.domain.service;

import com.omnip.transaction.domain.model.StoreMutations;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.transaction.domain.model.MutationReferenceType;
import com.omnip.transaction.domain.model.MutationType;
import com.omnip.shared.exception.InsufficientBalanceException;
import com.omnip.transaction.adapter.out.persistence.StoreMutationJpaRepository;
import com.omnip.onboarding.adapter.out.persistence.StoreJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class BalanceDomainService {

    private final StoreJpaRepository storeRepository;
    private final StoreMutationJpaRepository storeMutationRepository;

    public BalanceDomainService(StoreJpaRepository storeRepository, StoreMutationJpaRepository storeMutationRepository) {
        this.storeRepository = storeRepository;
        this.storeMutationRepository = storeMutationRepository;
    }

    @Transactional
    public StoreMutations deductBalance(UUID storeId, BigDecimal amount,
            MutationReferenceType referenceType,
            UUID referenceId, String description)
            throws InsufficientBalanceException {

        Stores store = storeRepository.findByIdWithPessimisticLock(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        BigDecimal currentBalance = store.getBalance();

        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Saldo tidak mencukupi. Saldo: " + currentBalance + ", dibutuhkan: " + amount);
        }

        BigDecimal newBalance = currentBalance.subtract(amount);

        StoreMutations mutation = new StoreMutations();
        mutation.setStore(store);
        mutation.setAmount(amount);
        mutation.setType(MutationType.DEBIT);
        mutation.setBalanceAfter(newBalance);
        mutation.setReferenceType(referenceType);
        mutation.setReferenceId(referenceId);
        mutation.setDescription(description);
        storeMutationRepository.save(mutation);

        store.setBalance(newBalance);
        storeRepository.save(store);

        log.info("DEBIT store={} amount={} balanceAfter={} ref={}:{}",
                storeId, amount, newBalance, referenceType, referenceId);

        return mutation;
    }

    @Transactional
    public StoreMutations addBalance(UUID storeId, BigDecimal amount,
            MutationReferenceType referenceType,
            UUID referenceId, String description) {

        Stores store = storeRepository.findByIdWithPessimisticLock(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));

        BigDecimal newBalance = store.getBalance().add(amount);

        StoreMutations mutation = new StoreMutations();
        mutation.setStore(store);
        mutation.setAmount(amount);
        mutation.setType(MutationType.CREDIT);
        mutation.setBalanceAfter(newBalance);
        mutation.setReferenceType(referenceType);
        mutation.setReferenceId(referenceId);
        mutation.setDescription(description);
        storeMutationRepository.save(mutation);

        store.setBalance(newBalance);
        storeRepository.save(store);

        log.info("CREDIT store={} amount={} balanceAfter={} ref={}:{}",
                storeId, amount, newBalance, referenceType, referenceId);

        return mutation;
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID storeId) {
        return storeRepository.findById(storeId)
                .map(Stores::getBalance)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
    }
}
