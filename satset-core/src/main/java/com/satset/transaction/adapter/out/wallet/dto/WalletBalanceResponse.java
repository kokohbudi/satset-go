package com.satset.transaction.adapter.out.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletBalanceResponse(
    UUID storeId,
    BigDecimal balance
) {}
