package com.satset.transaction.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

public record InquiryDTO(String customerName, BigDecimal bill, BigDecimal admin,
        BigDecimal markup, BigDecimal total, JsonNode desc) {
}
