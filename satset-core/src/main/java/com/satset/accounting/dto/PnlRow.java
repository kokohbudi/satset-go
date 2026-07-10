package com.satset.accounting.dto;

import java.math.BigDecimal;

public record PnlRow(String label, BigDecimal revenue, BigDecimal cogs, BigDecimal margin, long count) {
}
