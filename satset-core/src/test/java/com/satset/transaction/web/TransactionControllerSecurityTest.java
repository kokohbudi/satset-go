package com.satset.transaction.web;

import com.satset.shared.dto.UserDTO;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.PurchaseRequest;
import com.satset.transaction.service.TransactionDomainService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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
        UserDTO userDTO() {
            UserDTO u = new UserDTO();
            u.setStoreId(UUID.randomUUID());
            u.setWalletId("7001234567");
            return u;
        }

        @Bean
        TransactionController controller(TransactionDomainService s, WalletGateway w, UserDTO u) {
            return new TransactionController(s, w, u);
        }
    }

    @Autowired
    TransactionController controller;

    private PurchaseRequest purchaseRequest() {
        return new PurchaseRequest(UUID.randomUUID(), "081234567890");
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
}
