package com.satset.supplier.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Satu baris daftar-harga Digiflazz ({@code /v1/price-list}, cmd=prepaid).
 * Field subset yg dipakai admin; sisanya diabaikan Jackson.
 */
public record PriceListItem(
        @JsonProperty("product_name") String productName,
        String category,
        String brand,
        @JsonProperty("buyer_sku_code") String buyerSkuCode,
        long price,
        @JsonProperty("buyer_product_status") boolean buyerProductStatus,
        String stock,
        @JsonProperty("seller_name") String sellerName) {
}
