package com.satset.catalog.dto;

import com.satset.catalog.model.DenomType;

import java.math.BigDecimal;
import java.util.UUID;

/** Baris tabel "Semua Denom" — denom + konteks produk/kategori untuk tampilan lintas produk. */
public record DenomListItemDTO(
    UUID id,
    String code,
    String name,
    DenomType denomType,
    BigDecimal nominal,
    BigDecimal price,
    BigDecimal basePrice,
    boolean active,
    boolean deleted,
    UUID productId,
    String productName,
    String categoryName
) {}
