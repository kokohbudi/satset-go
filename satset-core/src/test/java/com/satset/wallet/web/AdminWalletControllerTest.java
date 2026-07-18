package com.satset.wallet.web;

import com.satset.shared.exception.GlobalExceptionHandler;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.wallet.model.MutationReferenceType;
import com.satset.wallet.service.account.WalletMutationResult;
import com.satset.wallet.service.account.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminWalletControllerTest {

    @Mock
    private WalletService walletService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminWalletController(walletService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void adjust_credits_with_ADJUSTMENT_type_and_returns_new_balance() throws Exception {
        when(walletService.credit(eq("7000000001"), eq(new BigDecimal("50000")), any(),
                eq(MutationReferenceType.ADJUSTMENT), any()))
                .thenReturn(new WalletMutationResult(UUID.randomUUID(), new BigDecimal("150000")));

        mockMvc.perform(post("/api/admin/wallets/7000000001/adjust")
                        .contentType("application/json")
                        .content("{\"amount\":50000,\"description\":\"manual inject\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.walletId").value("7000000001"))
                .andExpect(jsonPath("$.balance").value(150000));

        ArgumentCaptor<UUID> ref = ArgumentCaptor.forClass(UUID.class);
        verify(walletService).credit(eq("7000000001"), eq(new BigDecimal("50000")), ref.capture(),
                eq(MutationReferenceType.ADJUSTMENT), eq("manual inject"));
        assertThat(ref.getValue()).isNotNull();
    }

    @Test
    void adjust_unknown_wallet_returns_404() throws Exception {
        when(walletService.credit(any(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("WalletAccount", "9999999999"));

        mockMvc.perform(post("/api/admin/wallets/9999999999/adjust")
                        .contentType("application/json")
                        .content("{\"amount\":1000,\"description\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void adjust_rejects_non_positive_amount() throws Exception {
        mockMvc.perform(post("/api/admin/wallets/7000000001/adjust")
                        .contentType("application/json")
                        .content("{\"amount\":0,\"description\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mutations_returns_ledger_for_wallet() throws Exception {
        var m = com.satset.wallet.model.WalletMutationEntity.of("7000000001", new BigDecimal("50000"),
                com.satset.wallet.model.MutationType.CREDIT, new BigDecimal("150000"),
                UUID.randomUUID(), MutationReferenceType.ADJUSTMENT, "manual inject");
        when(walletService.getMutations("7000000001")).thenReturn(java.util.List.of(m));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/admin/wallets/7000000001/mutations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CREDIT"))
                .andExpect(jsonPath("$[0].referenceType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$[0].balanceAfter").value(150000));
    }
}
