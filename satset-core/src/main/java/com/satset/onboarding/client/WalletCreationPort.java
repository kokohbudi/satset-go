package com.satset.onboarding.client;

import java.util.UUID;

/**
 * Port for creating wallets in the Wallet service.
 * This is an outbound port that communicates with the external Wallet service.
 */
public interface WalletCreationPort {

    /**
     * Creates a new wallet for the given store.
     *
     * @param storeId the store ID
     * @return the generated wallet ID (format: 700xxxxxxx)
     */
    String createWallet(UUID storeId);
}
