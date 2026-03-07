package com.omnip.wallet.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletAccount(UUID id, UUID storeId, BigDecimal balance, Long version) {

    public static WalletAccount newAccount(UUID storeId) {
        return new WalletAccount(null, storeId, BigDecimal.ZERO, null);
    }

    public WalletAccount withBalance(BigDecimal newBalance) {
        return new WalletAccount(id, storeId, newBalance, version);
    }
}
