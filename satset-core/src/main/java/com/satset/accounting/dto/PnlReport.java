package com.satset.accounting.dto;

import java.time.LocalDate;
import java.util.List;

public record PnlReport(LocalDate from, LocalDate to, PnlSummary summary, List<PnlRow> byProduct) {
}
