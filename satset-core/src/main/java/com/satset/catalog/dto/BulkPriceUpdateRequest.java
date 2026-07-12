package com.satset.catalog.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Satu item bulk update harga jual. Validasi price di service (per-item result, bukan 400 batch). */
public record BulkPriceUpdateRequest(UUID id, BigDecimal price) {}
