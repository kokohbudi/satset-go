package com.satset.wallet.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Domain model for wallet account.
 * Uses walletId (String, format: 700xxxxxxx) as primary key.
 *
 * @param walletId Unique wallet identifier (10 chars, format: 700xxxxxxx)
 * @param storeId  Reference to store that owns this wallet
 * @param balance  Current wallet balance
 * @param version  Optimistic locking version
 */
public record WalletAccount(String walletId, UUID storeId, BigDecimal balance, Long version) {

    /**
     * Creates a new wallet account with zero balance.
     *
     * @param walletId the unique wallet identifier
     * @param storeId  the store that owns this wallet
     * @return new WalletAccount instance
     */
    public static WalletAccount newAccount(String walletId, UUID storeId) {
        return new WalletAccount(walletId, storeId, BigDecimal.ZERO, null);
    }

    /**
     * Returns a new WalletAccount with updated balance.
     *
     * @param newBalance the new balance amount
     * @return new WalletAccount instance with updated balance
     */
    public WalletAccount withBalance(BigDecimal newBalance) {
        return new WalletAccount(walletId, storeId, newBalance, version);
    }
}
