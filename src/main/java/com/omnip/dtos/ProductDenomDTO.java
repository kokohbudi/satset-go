package com.omnip.dtos;

import com.omnip.enums.DenomType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ProductDenomDTO {
    private UUID id;
    private String code;
    private String name;
    private DenomType denomType;

    // Pricing
    private BigDecimal nominal;
    private BigDecimal price;
    private BigDecimal adminFee;

    // Prepaid specific
    private Integer validityDays;
    private Long quotaMb;

    // Postpaid specific
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private boolean requiresInquiry;

    // Context
    private String productCode;
    private String productName;

    // Metadata (populated when getDenomWithMeta is called)
    private List<ProductDenomMetaDTO> metadata;
}