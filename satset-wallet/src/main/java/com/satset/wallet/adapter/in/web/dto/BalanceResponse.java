package com.satset.wallet.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceResponse(
        String walletId,
    BigDecimal balance,
    String currency,
    LocalDateTime asOf
) {
    public static BalanceResponse of(String walletId, BigDecimal balance) {
        return new BalanceResponse(walletId, balance, "IDR", LocalDateTime.now());
    }
}
