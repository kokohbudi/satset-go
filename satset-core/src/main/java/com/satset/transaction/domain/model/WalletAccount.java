package com.satset.transaction.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletAccount {

    private String walletId;
    private BigDecimal balance = BigDecimal.ZERO;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public WalletAccount(String walletId, BigDecimal balance) {
        this.walletId = walletId;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public WalletAccount() {}
}
