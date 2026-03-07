package com.satset.onboarding.domain.service;

import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.in.CreateStoreUseCase;
import com.satset.onboarding.domain.port.out.StoreRepositoryPort;
import com.satset.onboarding.domain.port.out.WalletCreationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Domain service for store operations.
 * Auto-creates wallet when a new store is created.
 */
@Service
public class StoreDomainService implements CreateStoreUseCase {

    private static final Logger log = LoggerFactory.getLogger(StoreDomainService.class);

    private final StoreRepositoryPort storeRepository;
    private final WalletCreationPort walletCreationPort;

    public StoreDomainService(StoreRepositoryPort storeRepository,
                              WalletCreationPort walletCreationPort) {
        this.storeRepository = storeRepository;
        this.walletCreationPort = walletCreationPort;
    }

    @Override
    public Stores createNewStore(Stores stores) {
        // First save the store to get an ID
        Stores savedStore = storeRepository.save(stores);

        // Auto-create wallet for the store
        try {
            String walletId = walletCreationPort.createWallet(savedStore.getId());
            savedStore.setWalletId(walletId);
            log.info("Wallet {} created for store {}", walletId, savedStore.getId());

            // Update store with walletId
            savedStore = storeRepository.save(savedStore);
        } catch (Exception e) {
            // Log error but don't fail store creation
            // Wallet can be created later manually or via retry mechanism
            log.error("Failed to auto-create wallet for store {}: {}", savedStore.getId(), e.getMessage());
        }

        return savedStore;
    }
}