package com.satset.onboarding.client;

import com.satset.wallet.service.account.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter for creating wallets via the in-process wallet service.
 * ponytail: 1-impl port, inline into StoreDomainService if it ever bothers you
 */
@Component
public class WalletCreationAdapter implements WalletCreationPort {

    private static final Logger log = LoggerFactory.getLogger(WalletCreationAdapter.class);

    private final WalletService walletService;

    public WalletCreationAdapter(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public String createWallet(UUID storeId) {
        log.info("Creating wallet for store {}", storeId);

        String walletId = walletService.createWallet().getWalletId();
        log.info("Wallet created successfully: {} for store {}", walletId, storeId);
        return walletId;
    }
}
