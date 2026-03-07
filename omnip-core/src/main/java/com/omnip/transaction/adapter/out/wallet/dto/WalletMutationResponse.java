package com.omnip.transaction.adapter.out.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletMutationResponse(
    UUID id,
    BigDecimal balanceAfter
) {}
