package com.satset.accounting.dto;

import java.math.BigDecimal;

public record PnlSummary(BigDecimal revenue, BigDecimal cogs, BigDecimal margin, long count) {
}
