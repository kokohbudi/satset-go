package com.satset.wallet.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BalanceResponse(
    UUID storeId,
    BigDecimal balance,
    String currency,
    LocalDateTime asOf
) {
    public static BalanceResponse of(UUID storeId, BigDecimal balance) {
        return new BalanceResponse(storeId, balance, "IDR", LocalDateTime.now());
    }
}