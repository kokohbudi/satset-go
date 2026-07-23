package com.satset.transaction.web;

import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.InquiryRequest;
import com.satset.transaction.dto.PayRequest;
import com.satset.transaction.dto.PlnInquiryRequest;
import com.satset.transaction.dto.PurchaseRequest;
import com.satset.transaction.service.postpaid.PostpaidService;
import com.satset.transaction.service.prepaid.PlnInquiryService;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Verifies @PreAuthorize role gates on TransactionController via Spring AOP method security.
 * Allowed = authz passes (any later NPE from mocked deps is irrelevant); denied = AccessDeniedException.
 */
@SpringJUnitConfig(TransactionControllerSecurityTest.Config.class)
class TransactionControllerSecurityTest {

    @EnableMethodSecurity
    @Configuration
    static class Config {
        @Bean
        TransactionDomainService transactionService() {
            return Mockito.mock(TransactionDomainService.class);
        }

        @Bean
        WalletGateway walletGateway() {
            return Mockito.mock(WalletGateway.class);
        }

        @Bean
        PostpaidService postpaidService() {
            return Mockito.mock(PostpaidService.class);
        }

        @Bean
        PlnInquiryService plnInquiryService() {
            return Mockito.mock(PlnInquiryService.class);
        }

        @Bean
        UserDTO userDTO() {
            UserDTO u = new UserDTO();
            u.setStoreId(UUID.randomUUID());
            u.setWalletId("7001234567");
            return u;
        }

        @Bean
        TransactionController controller(TransactionDomainService s, WalletGateway w, PostpaidService p,
                PlnInquiryService pln, UserDTO u) {
            return new TransactionController(s, w, p, pln, u);
        }
    }

    @Autowired
    TransactionController controller;

    private PurchaseRequest purchaseRequest() {
        return new PurchaseRequest(UUID.randomUUID(), "081234567890");
    }

    private InquiryRequest inquiryRequest() {
        return new InquiryRequest(UUID.randomUUID(), "530000000001", null);
    }

    private PayRequest payRequest() {
        return new PayRequest(UUID.randomUUID(), "530000000001", null, new BigDecimal("149000"));
    }

    private PlnInquiryRequest plnInquiryRequest() {
        return new PlnInquiryRequest("530000000001");
    }

    /** Authz passed: either no throw, or any throw other than AccessDeniedException (mocked-dep NPEs are fine). */
    private void assertAuthorized(ThrowingCallable call) {
        Throwable t = catchThrowable(call);
        if (t != null) {
            assertThat(t).isNotInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_transaction")
    void balance_allowed_withTransactionRole() {
        assertAuthorized(controller::getBalance);
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_purchase")
    void balance_denied_withoutTransactionRole() {
        assertThat(catchThrowable(controller::getBalance)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_purchase")
    void purchase_allowed_withPurchaseRole() {
        assertAuthorized(() -> controller.purchase(purchaseRequest()));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_transaction")
    void purchase_denied_withoutPurchaseRole() {
        assertThat(catchThrowable(() -> controller.purchase(purchaseRequest())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_purchase")
    void inquiry_allowed_withPurchaseRole() {
        assertAuthorized(() -> controller.inquiry(inquiryRequest()));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_transaction")
    void inquiry_denied_withoutPurchaseRole() {
        assertThat(catchThrowable(() -> controller.inquiry(inquiryRequest())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_purchase")
    void pay_allowed_withPurchaseRole() {
        assertAuthorized(() -> controller.pay(payRequest()));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_transaction")
    void pay_denied_withoutPurchaseRole() {
        assertThat(catchThrowable(() -> controller.pay(payRequest())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_purchase")
    void plnInquiry_allowed_withPurchaseRole() {
        assertAuthorized(() -> controller.plnInquiry(plnInquiryRequest()));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CLIENT_transaction")
    void plnInquiry_denied_withoutPurchaseRole() {
        assertThat(catchThrowable(() -> controller.plnInquiry(plnInquiryRequest())))
                .isInstanceOf(AccessDeniedException.class);
    }
}
