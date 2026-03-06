package com.omnip.transaction.domain.service;

import com.omnip.transaction.domain.model.StoreMutations;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.transaction.domain.model.MutationReferenceType;
import com.omnip.transaction.domain.model.MutationType;
import com.omnip.transaction.domain.port.in.BalanceManagementUseCase;
import com.omnip.shared.exception.InsufficientBalanceException;
import com.omnip.shared.exception.ResourceNotFoundException;
import com.omnip.transaction.domain.port.out.StoreMutationRepositoryPort;
import com.omnip.transaction.domain.port.out.StoreBalancePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class BalanceDomainService implements BalanceManagementUseCase {

        private final StoreBalancePort storeRepository;
        private final StoreMutationRepositoryPort storeMutationRepository;

        public BalanceDomainService(StoreBalancePort storeRepository,
                        StoreMutationRepositoryPort storeMutationRepository) {
                this.storeRepository = storeRepository;
                this.storeMutationRepository = storeMutationRepository;
        }

        @Override
        @Transactional
        public StoreMutations deductBalance(UUID storeId, BigDecimal amount,
                        MutationReferenceType referenceType,
                        UUID referenceId, String description)
                        throws InsufficientBalanceException {

                Stores store = storeRepository.findByIdWithPessimisticLock(storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));

                BigDecimal currentBalance = store.getBalance();

                if (currentBalance.compareTo(amount) < 0) {
                        throw new InsufficientBalanceException(
                                        "Saldo tidak mencukupi. Saldo: " + currentBalance + ", dibutuhkan: " + amount);
                }

                BigDecimal newBalance = currentBalance.subtract(amount);

                StoreMutations mutation = new StoreMutations();
                mutation.setStoreId(store.getId());
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

        @Override
        @Transactional
        public StoreMutations addBalance(UUID storeId, BigDecimal amount,
                        MutationReferenceType referenceType,
                        UUID referenceId, String description) {

                Stores store = storeRepository.findByIdWithPessimisticLock(storeId)
                                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));

                BigDecimal newBalance = store.getBalance().add(amount);

                StoreMutations mutation = new StoreMutations();
                mutation.setStoreId(store.getId());
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

        @Override
        @Transactional(readOnly = true)
        public BigDecimal getBalance(UUID storeId) {
                return storeRepository.findById(storeId)
                                .map(Stores::getBalance)
                                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        }
}
