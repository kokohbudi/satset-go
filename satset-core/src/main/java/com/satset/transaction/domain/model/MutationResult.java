package com.satset.transaction.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record MutationResult(
    UUID mutationId,
    BigDecimal balanceAfter
) {}
