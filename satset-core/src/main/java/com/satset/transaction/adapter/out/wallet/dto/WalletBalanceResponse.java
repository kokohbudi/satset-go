package com.satset.transaction.adapter.out.wallet.dto;

import java.math.BigDecimal;

public record WalletBalanceResponse(
        String walletId,
    BigDecimal balance
) {}
