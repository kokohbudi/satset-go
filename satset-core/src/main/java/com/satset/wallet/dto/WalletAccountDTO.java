package com.satset.wallet.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read view of a wallet account for the admin inject-saldo table. */
public record WalletAccountDTO(String walletId, BigDecimal balance, LocalDateTime updatedAt,
                               String storeName, String email) {
}
