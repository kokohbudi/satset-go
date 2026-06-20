package com.satset.catalog.dto;

import com.satset.catalog.model.DenomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateDenomRequest(
    @NotBlank @Size(max = 100) String code,
    @NotBlank @Size(max = 150) String name,
    @NotNull DenomType denomType,
    BigDecimal nominal,
    @NotNull @Positive BigDecimal price,
    BigDecimal basePrice,
    BigDecimal adminFee,
    Integer validityDays,
    Long quotaMb,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    boolean requiresInquiry,
    Integer stockAvailable,
    boolean active,
    int sortOrder
) {}
