package com.satset.catalog.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_denoms")
@EntityListeners(AuditingEntityListener.class)
@Data
public class ProductDenoms {
    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    private UUID productId;

    @Column(unique = true, nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DenomType denomType;

    // === Pricing ===
    @Column(precision = 15, scale = 2)
    private BigDecimal nominal;

    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    @Column(precision = 15, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal adminFee;

    // === Prepaid specific ===
    private Integer validityDays;

    private Long quotaMb;

    // === Postpaid specific ===
    @Column(precision = 15, scale = 2)
    private BigDecimal minAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @Column(nullable = false)
    private boolean requiresInquiry;

    // === Common ===
    private Integer stockAvailable;

    private boolean active;

    private boolean deleted;

    private int sortOrder;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    @Version
    private Long version;

    // Intentionally transient — metadata lives in ProductDenomMeta table.
    // Only populated via DenomDomainService.getDenomWithMeta(). Null otherwise.
    @Transient
    private List<ProductDenomMeta> metadata;
}
