package com.satset.transaction.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_accounts")
@EntityListeners(AuditingEntityListener.class)
@Data
public class WalletAccountJpaEntity {

    // wallet_id is the business key owned by satset-wallet service (format: 700xxxxxxx).
    // satset-core uses this as PK to align with satset-wallet's schema.
    @Id
    @Column(name = "wallet_id", updatable = false, nullable = false, length = 10)
    private String walletId;

    @Column(name = "store_id", columnDefinition = "uuid", nullable = false, unique = true)
    private UUID storeId;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}