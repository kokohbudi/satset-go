package com.satset.transaction.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class StoreMutations {

    private UUID id;
    private String walletId;
    private BigDecimal amount;
    private MutationType type;
    private BigDecimal balanceAfter;
    private MutationReferenceType referenceType;
    private UUID referenceId;
    private String description;
    private LocalDateTime createdAt;
    private Long version;
}