package com.satset.transaction.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class Transactions {

    private UUID id;
    private UUID storeId;
    private UUID productDenomId;
    private String denomName;
    private String productName;
    private String targetNumber;
    private BigDecimal price;
    private BigDecimal adminFee = BigDecimal.ZERO;
    private BigDecimal total;
    private TransactionStatus status = TransactionStatus.PENDING;
    private String providerRef;
    private String serialNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}