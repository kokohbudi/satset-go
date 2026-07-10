package com.satset.accounting.service;

import com.satset.accounting.dto.PnlReport;
import com.satset.accounting.dto.PnlRow;
import com.satset.accounting.dto.PnlSummary;
import com.satset.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountingService {

    private final TransactionRepository transactionRepository;

    public AccountingService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public PnlReport report(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay(); // half-open: includes all of `to`
        PnlSummary summary = transactionRepository.summarizePnl(start, end);
        List<PnlRow> byProduct = transactionRepository.summarizePnlByProduct(start, end);
        return new PnlReport(from, to, summary, byProduct);
    }
}
