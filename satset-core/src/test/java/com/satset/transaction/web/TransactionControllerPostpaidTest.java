package com.satset.transaction.web;

import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.GlobalExceptionHandler;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.InquiryDTO;
import com.satset.transaction.dto.InquiryRequest;
import com.satset.transaction.dto.PayRequest;
import com.satset.transaction.dto.TransactionDTO;
import com.satset.transaction.service.postpaid.PostpaidService;
import com.satset.transaction.service.prepaid.PlnInquiryService;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TransactionControllerPostpaidTest {

    private static final UUID DENOM_ID = UUID.randomUUID();

    private final TransactionDomainService txService = mock(TransactionDomainService.class);
    private final WalletGateway walletGateway = mock(WalletGateway.class);
    private final PostpaidService postpaidService = mock(PostpaidService.class);
    private final PlnInquiryService plnInquiryService = mock(PlnInquiryService.class);
    private final UserDTO userDTO = mock(UserDTO.class);

    private final TransactionController controller =
            new TransactionController(txService, walletGateway, postpaidService, plnInquiryService, userDTO);

    @Test
    void inquiryDelegatesToPostpaidService() throws Exception {
        InquiryDTO dto = new InquiryDTO("BUDI SANTOSO", new BigDecimal("145000"),
                new BigDecimal("2500"), new BigDecimal("1500"), new BigDecimal("149000"), null);
        when(postpaidService.inquiry(DENOM_ID, "530000000001", null)).thenReturn(dto);

        var response = controller.inquiry(new InquiryRequest(DENOM_ID, "530000000001", null));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(dto);
    }

    @Test
    void payDelegatesWithStoreAndWalletFromUser() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(userDTO.getStoreId()).thenReturn(storeId);
        when(userDTO.getWalletId()).thenReturn("wallet-1");
        TransactionDTO dto = new TransactionDTO(UUID.randomUUID(), "TRX010", storeId,
                "530000000001", "BUDI SANTOSO", "PLN Pascabayar", "PLN", new java.math.BigDecimal("145000"),
                new java.math.BigDecimal("4000"), new java.math.BigDecimal("149000"),
                com.satset.transaction.model.TransactionStatus.SUCCESS, "DF123",
                "STRUK/PLN/1234567890", java.time.LocalDateTime.now());
        when(postpaidService.pay(storeId, "wallet-1", DENOM_ID, "530000000001",
                null, new java.math.BigDecimal("149000"))).thenReturn(dto);

        var response = controller.pay(new com.satset.transaction.dto.PayRequest(
                DENOM_ID, "530000000001", null, new java.math.BigDecimal("149000")));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(dto);
        assertThat(response.getBody().customerName()).isEqualTo("BUDI SANTOSO");
    }

    /**
     * MockMvc + the real GlobalExceptionHandler advice (no full Spring context needed) —
     * proves Task 8's BusinessException("BILL_CHANGED", ...) -> 409 mapping is actually
     * wired end-to-end through the controller, not just unit-tested on the handler in isolation.
     */
    @Test
    void pay_billChanged_propagatesAs409() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(userDTO.getStoreId()).thenReturn(storeId);
        when(userDTO.getWalletId()).thenReturn("wallet-1");
        when(postpaidService.pay(eq(storeId), eq("wallet-1"), eq(DENOM_ID), eq("530000000001"),
                isNull(), any(BigDecimal.class)))
                .thenThrow(new BusinessException("BILL_CHANGED", "Tagihan berubah sejak pengecekan."));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        PayRequest request = new PayRequest(DENOM_ID, "530000000001", null, new BigDecimal("149000"));

        mockMvc.perform(post("/api/transactions/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BILL_CHANGED"));
    }
}
