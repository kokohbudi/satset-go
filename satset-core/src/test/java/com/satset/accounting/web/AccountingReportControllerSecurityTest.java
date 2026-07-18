package com.satset.accounting.web;

import com.satset.accounting.dto.PnlReport;
import com.satset.accounting.dto.PnlSummary;
import com.satset.accounting.service.report.AccountingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Verifies @PreAuthorize role gate on AccountingReportController via Spring AOP method security.
 * Allowed = authz passes and view renders; denied = AccessDeniedException.
 */
@SpringJUnitConfig(AccountingReportControllerSecurityTest.Config.class)
class AccountingReportControllerSecurityTest {

    @EnableMethodSecurity
    @Configuration
    static class Config {
        @Bean
        AccountingService accountingService() {
            AccountingService service = Mockito.mock(AccountingService.class);
            when(service.report(any(), any())).thenReturn(new PnlReport(
                    LocalDate.now(), LocalDate.now(),
                    new PnlSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L),
                    Collections.emptyList()));
            return service;
        }

        @Bean
        AccountingReportController controller(AccountingService accountingService) {
            return new AccountingReportController(accountingService);
        }
    }

    @Autowired
    AccountingReportController controller;

    /** Authz passed: either no throw, or any throw other than AccessDeniedException (mocked-dep NPEs are fine). */
    private void assertAuthorized(ThrowingCallable call) {
        Throwable t = catchThrowable(call);
        if (t != null) {
            assertThat(t).isNotInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    @WithMockUser(authorities = "ROLE_REALM_view_catalog")
    void pnlReport_forbidden_withoutReportsRole() {
        assertThat(catchThrowable(() -> controller.pnlReport(null, null, new ExtendedModelMap())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "ROLE_REALM_view_reports")
    void pnlReport_ok_withReportsRole() {
        ExtendedModelMap model = new ExtendedModelMap();
        assertAuthorized(() -> controller.pnlReport(null, null, model));
        assertThat(model.getAttribute("report")).isNotNull();
        assertThat(controller.pnlReport(null, null, model)).isEqualTo("pages/admin/pnl-report");
    }
}
