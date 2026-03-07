package com.satset.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletMutation(
        UUID id,
        UUID storeId,
        BigDecimal amount,
        MutationType mutationType,
        BigDecimal balanceAfter,
        MutationReferenceType referenceType,
        UUID referenceId,
        String description,
        LocalDateTime createdAt
) {
    public static WalletMutation of(UUID storeId, BigDecimal amount, MutationType mutationType,
            BigDecimal balanceAfter, UUID referenceId, MutationReferenceType referenceType,
            String description) {
        return new WalletMutation(null, storeId, amount, mutationType, balanceAfter,
                referenceType, referenceId, description, null);
    }
}
