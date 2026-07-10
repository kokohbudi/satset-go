package com.satset.supplier.model;

import java.math.BigDecimal;

/**
 * Satu baris banding harga beli: cost katalog ({@code dbCost} = basePrice) vs
 * cost Digiflazz ({@code dfCost}). {@code delta = dfCost - dbCost}.
 * Field bisa null tergantung {@link CompareStatus} (BARU: dbCost/delta null; HILANG: dfCost/delta null).
 */
public record PriceCompareRow(
        String buyerSku,
        String productName,
        String brand,
        String category,
        String seller,
        BigDecimal dbCost,
        BigDecimal dfCost,
        BigDecimal delta,
        java.util.UUID denomId,
        CompareStatus status) {
}
