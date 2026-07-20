package com.satset.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.satset.transaction.client.DigiflazzStatusMapper;
import com.satset.transaction.model.ProviderResponse;

import java.math.BigDecimal;

/**
 * Digiflazz buyer webhook payload — {@code {"data": {...}}}, per
 * developer.digiflazz.com/api/buyer/webhook/. Ignores unknown fields
 * (buyer_last_saldo, tele, wa, ...) — we only need what settles the transaction.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DigiflazzWebhookPayload(@JsonProperty("data") Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("ref_id") String refId,
            @JsonProperty("customer_no") String customerNo,
            @JsonProperty("buyer_sku_code") String buyerSkuCode,
            @JsonProperty("message") String message,
            @JsonProperty("status") String status,
            @JsonProperty("rc") String rc,
            @JsonProperty("sn") String sn,
            @JsonProperty("price") BigDecimal price) {

        public ProviderResponse toProviderResponse() {
            return new ProviderResponse(
                    DigiflazzStatusMapper.map(status(), rc()),
                    refId(),
                    emptyToNull(sn()),
                    message(),
                    price());
        }

        private static String emptyToNull(String s) {
            return (s == null || s.isBlank()) ? null : s;
        }
    }
}
