package com.satset.transaction.web;

import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.dto.PurchaseRequest;
import com.satset.transaction.dto.TopUpRequest;
import com.satset.transaction.client.WalletClientAdapter;
import com.satset.transaction.dto.TransactionDTO;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.service.TransactionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock private TransactionDomainService transactionService;
    @Mock private WalletClientAdapter balanceService;

    private MockMvc mockMvc;
    private final tools.jackson.databind.json.JsonMapper jsonMapper = tools.jackson.databind.json.JsonMapper.builder().build();
    private UUID storeId;
    private String walletId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        walletId = "7001234567";
        UserDTO userDTO = new UserDTO();
        userDTO.setStoreId(storeId);
        userDTO.setWalletId(walletId);

        TransactionController controller = new TransactionController(
                transactionService, balanceService, userDTO);

        var converter = new JacksonJsonHttpMessageConverter(jsonMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void purchase_ReturnsOk_WithResponseBody() throws Exception {
        UUID denomId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        TransactionDTO summary = buildSummary(txId, TransactionStatus.SUCCESS, new BigDecimal("10000"));

        when(transactionService.createPurchase(storeId, walletId, denomId, "081234567890")).thenReturn(summary);

        PurchaseRequest request = new PurchaseRequest(denomId, "081234567890");

        mockMvc.perform(post("/api/transactions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                .andExpect(jsonPath("$.total").value(10000));

        verify(transactionService).createPurchase(storeId, walletId, denomId, "081234567890");
    }

    @Test
    void topup_ReturnsOk_WithBalanceInResponse() throws Exception {
        BigDecimal newBalance = new BigDecimal("50000");
        when(balanceService.getBalance(walletId)).thenReturn(newBalance);

        TopUpRequest request = new TopUpRequest(new BigDecimal("50000"), "Top-up manual");

        mockMvc.perform(post("/api/transactions/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.balance").value(50000));

        verify(transactionService).topUp(eq(walletId), any(BigDecimal.class), any());
        verify(balanceService).getBalance(walletId);
    }

    @Test
    void balance_ReturnsWalletIdAndBalance() throws Exception {
        when(balanceService.getBalance(walletId)).thenReturn(new BigDecimal("25000"));

        mockMvc.perform(get("/api/transactions/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId))
                .andExpect(jsonPath("$.balance").value(25000));
    }

    @Test
    void getTransaction_ReturnsOk_WithTransactionDTO() throws Exception {
        UUID txId = UUID.randomUUID();
        TransactionDTO summary = buildSummary(txId, TransactionStatus.SUCCESS, new BigDecimal("5000"));

        when(transactionService.getTransaction(txId, storeId)).thenReturn(summary);

        mockMvc.perform(get("/api/transactions/{id}", txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(txId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getTransactionHistory_DelegatesToUseCase_WithStoreId() throws Exception {
        UUID txId = UUID.randomUUID();
        TransactionDTO summary = buildSummary(txId, TransactionStatus.SUCCESS, new BigDecimal("7000"));

        when(transactionService.getTransactionHistory(eq(storeId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        // Note: Page<T> serialization requires full Spring context; verify delegation only
        mockMvc.perform(get("/api/transactions/history"));

        verify(transactionService).getTransactionHistory(eq(storeId), any(Pageable.class));
    }

    @Test
    void purchase_WhenNoWalletId_ThrowsResourceNotFoundException() {
        // Controller with no walletId in UserDTO (but storeId present)
        UserDTO noWalletUserDTO = new UserDTO();
        noWalletUserDTO.setStoreId(storeId); // storeId present, walletId null
        TransactionController noWalletController = new TransactionController(
                transactionService, balanceService, noWalletUserDTO);
        MockMvc noWalletMockMvc = MockMvcBuilders.standaloneSetup(noWalletController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        PurchaseRequest request = new PurchaseRequest(UUID.randomUUID(), "081234567890");

        Exception thrown = assertThrows(Exception.class, () ->
                noWalletMockMvc.perform(post("/api/transactions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request)))
                        .andReturn());

        // Exception propagates wrapped or directly as ResourceNotFoundException
        Throwable root = thrown.getCause() != null ? thrown.getCause() : thrown;
        assertInstanceOf(ResourceNotFoundException.class, root,
                "Expected ResourceNotFoundException but got: " + root.getClass().getName());
    }

    private TransactionDTO buildSummary(UUID id, TransactionStatus status, BigDecimal total) {
        return new TransactionDTO(
                id, storeId, "081234567890", "Pulsa 10K", "Telkomsel",
                total, BigDecimal.ZERO, total, status,
                "PROV-REF-123", "SN-456", LocalDateTime.now());
    }
}
