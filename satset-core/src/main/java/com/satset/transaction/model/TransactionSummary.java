package com.satset.transaction.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain-level read model for transaction data.
 * Used by use case ports to avoid leaking JPA entities
 * across the port boundary.
 */
public record TransactionSummary(
        UUID id,
        UUID storeId,
        String targetNumber,
        String denomName,
        String productName,
        BigDecimal price,
        BigDecimal adminFee,
        BigDecimal total,
        TransactionStatus status,
        String providerRef,
        String serialNumber,
        LocalDateTime createdAt) {
}
