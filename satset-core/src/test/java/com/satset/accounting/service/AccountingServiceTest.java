package com.satset.accounting.service;

import com.satset.accounting.dto.PnlReport;
import com.satset.accounting.dto.PnlRow;
import com.satset.accounting.dto.PnlSummary;
import com.satset.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountingServiceTest {

    @Mock TransactionRepository transactionRepository;
    @InjectMocks AccountingService accountingService;

    @Test
    void report_usesHalfOpenRange_andPassesThroughAggregates() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 10);
        LocalDateTime start = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 11, 0, 0); // to + 1 day, exclusive

        PnlSummary summary = new PnlSummary(new BigDecimal("17500"), new BigDecimal("16000"),
                new BigDecimal("1500"), 2);
        List<PnlRow> rows = List.of(new PnlRow("Telkomsel", new BigDecimal("11000"),
                new BigDecimal("10000"), new BigDecimal("1000"), 1));
        when(transactionRepository.summarizePnl(start, end)).thenReturn(summary);
        when(transactionRepository.summarizePnlByProduct(start, end)).thenReturn(rows);

        PnlReport report = accountingService.report(from, to);

        assertThat(report.from()).isEqualTo(from);
        assertThat(report.to()).isEqualTo(to);
        assertThat(report.summary()).isEqualTo(summary);
        assertThat(report.byProduct()).isEqualTo(rows);
    }
}
