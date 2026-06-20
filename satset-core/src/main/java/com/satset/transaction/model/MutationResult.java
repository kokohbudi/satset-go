package com.satset.transaction.model;

import java.math.BigDecimal;
import java.util.UUID;

public record MutationResult(
    UUID mutationId,
    BigDecimal balanceAfter
) {}
