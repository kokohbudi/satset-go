package com.satset.transaction.dto;

import java.math.BigDecimal;

public record WalletBalanceResponse(
        String walletId,
    BigDecimal balance
) {}
