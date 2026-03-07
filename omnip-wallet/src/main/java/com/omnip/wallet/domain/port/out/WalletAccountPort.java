package com.omnip.wallet.domain.port.out;

import com.omnip.wallet.domain.model.WalletAccount;

import java.util.Optional;
import java.util.UUID;

public interface WalletAccountPort {

    Optional<WalletAccount> findByStoreId(UUID storeId);

    Optional<WalletAccount> findByStoreIdWithLock(UUID storeId);

    WalletAccount save(WalletAccount account);
}