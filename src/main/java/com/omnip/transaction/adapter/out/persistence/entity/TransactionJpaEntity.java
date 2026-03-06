package com.omnip.transaction.adapter.out.persistence.entity;

import com.omnip.transaction.domain.model.TransactionStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class TransactionJpaEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "store_id", nullable = false, columnDefinition = "uuid")
    private UUID storeId;

    @Column(name = "product_denom_id", nullable = false, columnDefinition = "uuid")
    private UUID productDenomId;

    @Column(nullable = true, length = 150)
    private String denomName;

    @Column(length = 100)
    private String productName;

    @Column(nullable = false, length = 50)
    private String targetNumber;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(precision = 10, scale = 2)
    private BigDecimal adminFee = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 100)
    private String providerRef;

    @Column(length = 100)
    private String serialNumber;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version;
}