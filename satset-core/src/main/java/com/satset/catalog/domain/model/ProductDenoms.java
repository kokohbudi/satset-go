package com.satset.catalog.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class ProductDenoms {
    private UUID id;
    private UUID productId;
    private String code;
    private String name;
    private DenomType denomType;

    // === Pricing ===
    private BigDecimal nominal;
    private BigDecimal price;
    private BigDecimal basePrice;
    private BigDecimal adminFee;

    // === Prepaid specific ===
    private Integer validityDays;
    private Long quotaMb;

    // === Postpaid specific ===
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private boolean requiresInquiry;

    // === Common ===
    private Integer stockAvailable;
    private boolean active;
    private boolean deleted;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;

    // Intentionally transient — metadata lives in ProductDenomMeta table.
    // Only populated via DenomDomainService.getDenomWithMeta(). Null otherwise.
    private List<ProductDenomMeta> metadata;
}