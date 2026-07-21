package com.satset.transaction.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satset.catalog.model.DenomType;
import com.satset.catalog.repository.DenomRepository;
import com.satset.identity.client.KeycloakIdentityPort;
import com.satset.onboarding.repository.StoreRepository;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.dto.PurchaseRequest;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.model.*;
import com.satset.transaction.client.ProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class PurchaseFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private StoreRepository storeJpaRepo;

    @MockitoBean
    private DenomRepository productDenomRepository;

    @MockitoBean
    private WalletGateway balanceManagementUseCase;

    @MockitoBean
    private ProviderPort providerService;

    @MockitoBean
    private KeycloakIdentityPort keycloakIdentityPort;

    @MockitoBean
    private UserDTO userDTO;

    private UUID storeId;
    private String walletId;
    private UUID denomId;
    private DenomInfo denomInfo;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        storeId = UUID.randomUUID();
        walletId = "7001234567";
        denomId = UUID.randomUUID();

        // Setup denom info (shared kernel value object used by TransactionDomainService)
        denomInfo = new DenomInfo(denomId, "PULSA10", "Telkomsel 10K", "Telkomsel",
                new BigDecimal("10000.00"), BigDecimal.ZERO, new BigDecimal("9000.00"), true, false,
                false, DenomType.FIXED_DENOM, null, null);

        // BalanceManagementUseCase mock — TransactionDomainService uses this for balance operations
        doNothing().when(balanceManagementUseCase).deductBalance(any(), any(), any(), any());
        doNothing().when(balanceManagementUseCase).refundBalance(any(), any(), any(), any());
        doReturn(new BigDecimal("100000.00")).when(balanceManagementUseCase).getBalance(any());

        // UserDTO provides store/wallet context to controller
        when(userDTO.getStoreId()).thenReturn(storeId);
        when(userDTO.getWalletId()).thenReturn(walletId);


        // Denom lookup (service uses findDenomInfoById, not findById)
        when(productDenomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denomInfo));

        // Idempotency check: no duplicate
        when(transactionRepository
                .existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                        any(), any(), anyString(), any(), any()))
                .thenReturn(false);

        // Transaction save: assign UUID if not set
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });
    }

    // ==============================================
    // SKENARIO 1: Happy Path — SUCCESS
    // ==============================================

    @Test
    void whenPurchase_withSufficientBalance_andProviderSuccess_thenTransactionSuccess() throws Exception {
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.SUCCESS, "REF-123", "SN-123", "Success", null));

        PurchaseRequest request = new PurchaseRequest(denomId, "081234567890");

        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT_purchase")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TransactionStatus.SUCCESS.name()))
                .andExpect(jsonPath("$.providerRef").value("REF-123"))
                .andExpect(jsonPath("$.serialNumber").value("SN-123"))
                // ref_no = YYYYMMDD + 5-digit counter, surfaced to the client as the invoice number
                .andExpect(jsonPath("$.refNo").value(org.hamcrest.Matchers.matchesPattern("\\d{8}\\d{5,}")));

        // the SAME ref_no (not the UUID) is what went to Digiflazz as ref_id
        verify(providerService, times(1))
                .sendTransaction(eq("081234567890"), eq("PULSA10"), eq(new BigDecimal("10000.00")), matches("\\d{8}\\d{5,}"));
    }

    // ==============================================
    // SKENARIO 2: Saldo Tidak Cukup — HTTP 422
    // ==============================================

    @Test
    void whenPurchase_withInsufficientBalance_thenReturn422AndProviderNotCalled() throws Exception {
        doThrow(new InsufficientBalanceException("Saldo tidak mencukupi"))
                .when(balanceManagementUseCase).deductBalance(any(), any(), any(), any());

        PurchaseRequest request = new PurchaseRequest(denomId, "081234567890");

        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT_purchase")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").exists());

        verify(providerService, never())
                .sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString());
    }

    // ==============================================
    // SKENARIO 3: Provider Gagal — REFUNDED
    // ==============================================

    @Test
    void whenPurchase_withProviderFailure_thenTransactionRefunded() throws Exception {
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.FAILED, null, null, "Timeout Biller", null));

        PurchaseRequest request = new PurchaseRequest(denomId, "081234567890");

        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT_purchase")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TransactionStatus.REFUNDED.name()));

        verify(providerService, times(1))
                .sendTransaction(eq("081234567890"), eq("PULSA10"), eq(new BigDecimal("10000.00")), anyString());

        // deductBalance (purchase) + refundBalance (refund) both called
        verify(balanceManagementUseCase, times(1)).deductBalance(eq(walletId), any(), any(), any());
        verify(balanceManagementUseCase, times(1)).refundBalance(eq(walletId), any(), any(), any());
    }

    // ==============================================
    // @LogContext weaving: MDC logctx=Topup harus aktif selama createPurchase
    // (bukti log service ke-route ke logs/Topup/). Capture MDC di downstream mock.
    // ==============================================
    @Test
    void createPurchase_runsWithinTopupLogContext() throws Exception {
        String[] captured = new String[1];
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenAnswer(inv -> {
                    captured[0] = org.slf4j.MDC.get("logctx");
                    return new ProviderResponse(ProviderStatus.SUCCESS, "REF-1", "SN-1", "Success", null);
                });

        PurchaseRequest request = new PurchaseRequest(denomId, "081234567890");
        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT_purchase")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(captured[0]).isEqualTo("Topup");
    }
}
