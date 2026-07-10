package com.satset.accounting.web;

import com.satset.accounting.dto.PnlReport;
import com.satset.accounting.service.AccountingService;
import com.satset.shared.constant.OmniConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@Slf4j
public class AccountingReportController {

    private final AccountingService accountingService;

    public AccountingReportController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    @GetMapping("/admin/reports/pnl")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_REPORTS + "')")
    public String pnlReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        LocalDate toDate = (to != null) ? to : LocalDate.now();
        LocalDate fromDate = (from != null) ? from : toDate.withDayOfMonth(1);
        if (fromDate.isAfter(toDate)) {
            fromDate = toDate.withDayOfMonth(1);
        }

        log.info("Accessing admin P&L report page: {} - {}", fromDate, toDate);

        PnlReport report = accountingService.report(fromDate, toDate);
        model.addAttribute("currentPage", "reports");
        model.addAttribute("breadcrumb", "Laba Rugi");
        model.addAttribute("report", report);
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
        return "pages/admin/pnl-report";
    }
}
