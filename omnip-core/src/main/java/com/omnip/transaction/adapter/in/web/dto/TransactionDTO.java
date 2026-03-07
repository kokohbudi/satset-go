package com.omnip.transaction.adapter.in.web.dto;

import com.omnip.transaction.domain.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionDTO(
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
