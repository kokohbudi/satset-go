package com.satset.onboarding.service.store;

import com.satset.onboarding.repository.StoreRepository;
import com.satset.onboarding.model.Stores;
import com.satset.wallet.service.account.WalletService;
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
    private final WalletService walletService;

    public StoreDomainService(StoreRepository storeRepository,
                              WalletService walletService) {
        this.storeRepository = storeRepository;
        this.walletService = walletService;
    }

    public Stores createNewStore(Stores stores) {
        // Save first to get the persisted ID from DB
        Stores saved = storeRepository.save(stores);

        try {
            String walletId = walletService.createWallet().getWalletId();
            saved.setWalletId(walletId);
            log.info("Wallet {} created for store {}", walletId, saved.getId());
            return storeRepository.save(saved);
        } catch (Exception e) {
            log.error("Failed to auto-create wallet for store {}: {}", saved.getId(), e.getMessage());
            return saved;
        }
    }
}