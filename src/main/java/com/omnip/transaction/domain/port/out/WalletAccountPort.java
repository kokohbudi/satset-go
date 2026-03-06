package com.omnip.transaction.domain.port.out;

import com.omnip.transaction.domain.model.WalletAccount;
import java.util.Optional;
import java.util.UUID;

public interface WalletAccountPort {
    
    Optional<WalletAccount> findByStoreId(UUID storeId);
    
    Optional<WalletAccount> findByStoreIdWithLock(UUID storeId);
    
    WalletAccount save(WalletAccount walletAccount);
}