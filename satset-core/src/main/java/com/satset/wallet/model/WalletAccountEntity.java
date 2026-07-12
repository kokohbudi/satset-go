package com.satset.wallet.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for wallet account.
 * Uses wallet_id (String, format: 700xxxxxxx) as primary key.
 */
@Entity
@Table(name = "wallet_accounts", schema = "satset_wallet")
@Getter
@Setter
public class WalletAccountEntity {

    @Id
    @Column(name = "wallet_id", nullable = false, updatable = false, length = 10)
    private String walletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /**
     * Creates a new wallet account with zero balance.
     *
     * @param walletId the unique wallet identifier
     * @return new WalletAccountEntity instance
     */
    public static WalletAccountEntity newAccount(String walletId) {
        WalletAccountEntity entity = new WalletAccountEntity();
        entity.setWalletId(walletId);
        entity.setBalance(BigDecimal.ZERO);
        return entity;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}