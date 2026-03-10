package com.satset.transaction.adapter.out.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletMutationRequest(
        String walletId,
    BigDecimal amount,
    UUID referenceId,
    String referenceType,
    String description
) {}
