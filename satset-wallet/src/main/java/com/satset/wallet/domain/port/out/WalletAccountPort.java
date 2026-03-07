package com.satset.wallet.domain.port.out;

import com.satset.wallet.domain.model.WalletAccount;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for wallet account persistence operations.
 */
public interface WalletAccountPort {

    Optional<WalletAccount> findByStoreId(UUID storeId);

    Optional<WalletAccount> findByStoreIdWithLock(UUID storeId);

    Optional<WalletAccount> findByWalletId(String walletId);

    WalletAccount save(WalletAccount account);

    boolean existsByWalletId(String walletId);
}