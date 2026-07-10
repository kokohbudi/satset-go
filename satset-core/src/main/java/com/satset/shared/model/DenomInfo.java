package com.satset.shared.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shared Kernel value object untuk informasi denom.
 * Digunakan untuk komunikasi antar bounded contexts (Transaction <-> Catalog).
 * 
 * Ini adalah read-only snapshot dari denom data yang diperlukan oleh
 * Transaction context tanpa perlu mengakses Catalog's domain model directly.
 */
public record DenomInfo(
    UUID id,
    String code,
    String name,
    String productName,
    BigDecimal price,
    BigDecimal adminFee,
    BigDecimal basePrice,
    boolean active,
    boolean deleted
) {
    /**
     * Calculate total price (price + admin fee).
     */
    public BigDecimal total() {
        BigDecimal fee = adminFee != null ? adminFee : BigDecimal.ZERO;
        return price.add(fee);
    }
    
    /**
     * Check if denom is available for purchase.
     */
    public boolean isAvailable() {
        return active && !deleted;
    }
}