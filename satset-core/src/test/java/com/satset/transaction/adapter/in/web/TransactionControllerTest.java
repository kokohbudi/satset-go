package com.satset.transaction.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.transaction.adapter.in.web.dto.PurchaseRequest;
import com.satset.transaction.adapter.in.web.dto.TopUpRequest;
import com.satset.transaction.domain.model.TransactionStatus;
import com.satset.transaction.domain.model.TransactionSummary;
import com.satset.transaction.domain.port.in.BalanceManagementUseCase;
import com.satset.transaction.domain.port.in.PurchaseUseCase;
import com.satset.transaction.domain.port.in.TopUpUseCase;
import com.satset.transaction.domain.port.in.TransactionQueryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock private PurchaseUseCase purchaseUseCase;
    @Mock private TopUpUseCase topUpUseCase;
    @Mock private TransactionQueryUseCase transactionQueryUseCase;
    @Mock private BalanceManagementUseCase balanceManagementUseCase;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UUID storeId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO();
        userDTO.setStoreId(storeId);

        TransactionController controller = new TransactionController(
                purchaseUseCase, topUpUseCase, transactionQueryUseCase,
                balanceManagementUseCase, userDTO);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void purchase_ReturnsOk_WithResponseBody() throws Exception {
        UUID denomId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        TransactionSummary summary = buildSummary(txId, TransactionStatus.SUCCESS, new BigDecimal("10000"));

        when(purchaseUseCase.createPurchase(storeId, denomId, "081234567890")).thenReturn(summary);

        PurchaseRequest request = new PurchaseRequest(denomId, "081234567890");

        mockMvc.perform(post("/api/transactions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionId").value(txId.toString()))
                .andExpect(jsonPath("$.total").value(10000));

        verify(purchaseUseCase).createPurchase(storeId, denomId, "081234567890");
    }

    @Test
    void topup_ReturnsOk_WithBalanceInResponse() throws Exception {
        BigDecimal newBalance = new BigDecimal("50000");
        when(balanceManagementUseCase.getBalance(storeId)).thenReturn(newBalance);

        TopUpRequest request = new TopUpRequest(new BigDecimal("50000"), "Top-up manual");

        mockMvc.perform(post("/api/transactions/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.balance").value(50000));

        verify(topUpUseCase).topUp(eq(storeId), any(BigDecimal.class), any());
        verify(balanceManagementUseCase).getBalance(storeId);
    }

    @Test
    void balance_ReturnsStoreIdAndBalance() throws Exception {
        when(balanceManagementUseCase.getBalance(storeId)).thenReturn(new BigDecimal("25000"));

        mockMvc.perform(get("/api/transactions/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.balance").value(25000));
    }

    @Test
    void getTransaction_ReturnsOk_WithTransactionDTO() throws Exception {
        UUID txId = UUID.randomUUID();
        TransactionSummary summary = buildSummary(txId, TransactionStatus.SUCCESS, new BigDecimal("5000"));

        when(transactionQueryUseCase.getTransaction(txId, storeId)).thenReturn(summary);

        mockMvc.perform(get("/api/transactions/{id}", txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(txId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void getTransactionHistory_DelegatesToUseCase_WithStoreId() throws Exception {
        UUID txId = UUID.randomUUID();
        TransactionSummary summary = buildSummary(txId, TransactionStatus.SUCCESS, new BigDecimal("7000"));

        when(transactionQueryUseCase.getTransactionHistory(eq(storeId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(summary)));

        // Note: Page<T> serialization requires full Spring context; verify delegation only
        mockMvc.perform(get("/api/transactions/history"));

        verify(transactionQueryUseCase).getTransactionHistory(eq(storeId), any(Pageable.class));
    }

    @Test
    void purchase_WhenNoStoreId_ThrowsResourceNotFoundException() throws Exception {
        // Controller with no storeId in UserDTO
        UserDTO emptyUserDTO = new UserDTO(); // storeId is null
        TransactionController noStoreController = new TransactionController(
                purchaseUseCase, topUpUseCase, transactionQueryUseCase,
                balanceManagementUseCase, emptyUserDTO);
        MockMvc noStoreMockMvc = MockMvcBuilders.standaloneSetup(noStoreController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        PurchaseRequest request = new PurchaseRequest(UUID.randomUUID(), "081234567890");

        Exception thrown = assertThrows(Exception.class, () ->
                noStoreMockMvc.perform(post("/api/transactions/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andReturn());

        // Exception propagates wrapped or directly as ResourceNotFoundException
        Throwable root = thrown.getCause() != null ? thrown.getCause() : thrown;
        assertTrue(root instanceof ResourceNotFoundException,
                "Expected ResourceNotFoundException but got: " + root.getClass().getName());
    }

    private TransactionSummary buildSummary(UUID id, TransactionStatus status, BigDecimal total) {
        return new TransactionSummary(
                id, storeId, "081234567890", "Pulsa 10K", "Telkomsel",
                total, BigDecimal.ZERO, total, status,
                "PROV-REF-123", "SN-456", LocalDateTime.now());
    }
}
