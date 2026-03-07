package com.satset.wallet.adapter.out.persistence.entity;

import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.MutationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallet_mutations", indexes = {
    @Index(name = "idx_wallet_mutations_store_id", columnList = "store_id"),
    @Index(name = "idx_wallet_mutations_created_at", columnList = "created_at"),
    @Index(name = "idx_wallet_mutations_reference", columnList = "reference_id, reference_type")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_mutations_reference", columnNames = {"reference_id", "reference_type", "mutation_type"})
})
@Getter
@Setter
public class WalletMutationEntity {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MutationType mutationType;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private MutationReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}