package com.satset.transaction.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

/** Read model for postpaid bill inquiry — mirrors Digiflazz's inq-pasca response, provider-agnostic. */
public record InquiryResult(String customerName, BigDecimal bill, BigDecimal admin,
        String rc, String message, JsonNode desc) {

    public boolean ok() {
        return "00".equals(rc);
    }
}
