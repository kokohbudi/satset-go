package com.omnip.wallet.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletMutationResult(
    UUID mutationId,
    BigDecimal newBalance
) {}