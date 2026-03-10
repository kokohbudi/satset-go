package com.satset.wallet.domain.port.out;

import com.satset.wallet.domain.model.WalletAccount;

import java.util.Optional;

/**
 * Port for wallet account persistence operations.
 */
public interface WalletAccountPort {

    Optional<WalletAccount> findByWalletId(String walletId);

    Optional<WalletAccount> findByWalletIdWithLock(String walletId);

    WalletAccount save(WalletAccount account);

    boolean existsByWalletId(String walletId);
}
