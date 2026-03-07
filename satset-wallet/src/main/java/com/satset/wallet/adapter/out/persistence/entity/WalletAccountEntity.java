package com.satset.wallet.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for wallet account.
 * Uses wallet_id (String, format: 700xxxxxxx) as primary key.
 */
@Entity
@Table(name = "wallet_accounts")
@Getter
@Setter
public class WalletAccountEntity {

    @Id
    @Column(name = "wallet_id", nullable = false, updatable = false, length = 10)
    private String walletId;

    @Column(name = "store_id", nullable = false, unique = true)
    private UUID storeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

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