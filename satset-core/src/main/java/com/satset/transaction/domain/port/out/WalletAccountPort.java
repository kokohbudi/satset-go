package com.satset.transaction.domain.port.out;

import com.satset.transaction.domain.model.WalletAccount;

import java.util.Optional;

public interface WalletAccountPort {

    Optional<WalletAccount> findByWalletId(String walletId);

    Optional<WalletAccount> findByWalletIdWithLock(String walletId);

    WalletAccount save(WalletAccount walletAccount);

    long count();
}
