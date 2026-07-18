package com.satset.wallet.service.account;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletMutationResult(
    UUID mutationId,
    BigDecimal newBalance
) {}