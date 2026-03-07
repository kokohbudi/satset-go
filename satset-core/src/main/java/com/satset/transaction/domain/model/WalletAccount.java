package com.satset.transaction.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class WalletAccount {

    private UUID id;
    private UUID storeId;
    private BigDecimal balance = BigDecimal.ZERO;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public WalletAccount(UUID storeId, BigDecimal balance) {
        this.storeId = storeId;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public WalletAccount() {}
}