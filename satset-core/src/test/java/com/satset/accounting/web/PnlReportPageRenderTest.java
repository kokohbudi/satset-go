package com.satset.accounting.web;

import com.satset.accounting.dto.PnlReport;
import com.satset.accounting.dto.PnlRow;
import com.satset.accounting.dto.PnlSummary;
import com.satset.accounting.service.AccountingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renders /admin/reports/pnl through the real Thymeleaf engine (unlike
 * {@link AccountingReportControllerSecurityTest}, which calls the controller method directly and
 * never exercises the view). Catches template-syntax errors (malformed ${...}, bad
 * #numbers.formatInteger usage, etc.) that would otherwise only surface on first real page load.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class PnlReportPageRenderTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean
    private AccountingService accountingService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void pnlReport_withRows_renders200AndKnownLabels() throws Exception {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 9);
        PnlSummary summary = new PnlSummary(
                new BigDecimal("1500000"), new BigDecimal("1200000"), new BigDecimal("300000"), 42L);
        List<PnlRow> byProduct = List.of(
                new PnlRow("TELKOMSEL", new BigDecimal("900000"), new BigDecimal("700000"),
                        new BigDecimal("200000"), 25L),
                new PnlRow("XL", new BigDecimal("600000"), new BigDecimal("500000"),
                        new BigDecimal("100000"), 17L));
        when(accountingService.report(any(), any())).thenReturn(new PnlReport(from, to, summary, byProduct));

        mockMvc.perform(get("/admin/reports/pnl")
                        .with(user("tester").authorities(new SimpleGrantedAuthority("ROLE_REALM_view_reports"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Laba Rugi")))
                .andExpect(content().string(containsString("Pendapatan")))
                .andExpect(content().string(containsString("Margin")))
                .andExpect(content().string(containsString("TELKOMSEL")));
    }

    @Test
    void pnlReport_withEmptyByProduct_renders200AndEmptyStateRow() throws Exception {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 9);
        PnlSummary summary = new PnlSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
        when(accountingService.report(any(), any()))
                .thenReturn(new PnlReport(from, to, summary, Collections.emptyList()));

        mockMvc.perform(get("/admin/reports/pnl")
                        .with(user("tester").authorities(new SimpleGrantedAuthority("ROLE_REALM_view_reports"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Laba Rugi")))
                .andExpect(content().string(containsString("Belum ada transaksi sukses")));
    }
}
