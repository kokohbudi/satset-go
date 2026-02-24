package com.omnip.entities;

import com.omnip.enums.TransactionStatus;
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
public class Transactions {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Stores store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_denom_id", nullable = false)
    private ProductDenoms productDenom;

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
}
