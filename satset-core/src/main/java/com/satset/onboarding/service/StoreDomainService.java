package com.satset.onboarding.service;

import com.satset.onboarding.repository.StoreRepository;
import com.satset.onboarding.model.Stores;
import com.satset.onboarding.client.WalletCreationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


/**
 * Domain service for store operations.
 * Auto-creates wallet when a new store is created.
 */
@Service
public class StoreDomainService {

    private static final Logger log = LoggerFactory.getLogger(StoreDomainService.class);

    private final StoreRepository storeRepository;
    private final WalletCreationPort walletCreationPort;

    public StoreDomainService(StoreRepository storeRepository,
                              WalletCreationPort walletCreationPort) {
        this.storeRepository = storeRepository;
        this.walletCreationPort = walletCreationPort;
    }

    public Stores createNewStore(Stores stores) {
        // Save first to get the persisted ID from DB
        Stores saved = storeRepository.save(stores);

        try {
            String walletId = walletCreationPort.createWallet(saved.getId());
            saved.setWalletId(walletId);
            log.info("Wallet {} created for store {}", walletId, saved.getId());
            return storeRepository.save(saved);
        } catch (Exception e) {
            log.error("Failed to auto-create wallet for store {}: {}", saved.getId(), e.getMessage());
            return saved;
        }
    }
}