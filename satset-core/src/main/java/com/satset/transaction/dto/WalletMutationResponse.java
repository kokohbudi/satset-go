package com.satset.transaction.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletMutationResponse(
    UUID id,
    BigDecimal balanceAfter
) {}
