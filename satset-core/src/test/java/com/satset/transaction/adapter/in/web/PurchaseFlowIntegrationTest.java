package com.satset.transaction.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satset.catalog.adapter.out.persistence.DenomRepository;
import com.satset.identity.domain.port.out.KeycloakIdentityPort;
import com.satset.onboarding.adapter.out.persistence.StoreRepository;
import com.satset.onboarding.domain.port.out.KeycloakOrganizationPort;
import com.satset.onboarding.domain.port.out.WalletCreationPort;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.adapter.in.web.dto.PurchaseRequest;
import com.satset.transaction.domain.model.*;
import com.satset.transaction.domain.port.in.BalanceManagementUseCase;
import com.satset.transaction.domain.port.out.ProviderPort;
import com.satset.transaction.domain.port.out.StoreMutationRepositoryPort;
import com.satset.transaction.domain.port.out.TransactionRepositoryPort;
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

    // Mock the port interfaces (adapters implement these)
    @MockitoBean
    private TransactionRepositoryPort transactionRepository;

    @MockitoBean
    private StoreRepository storeJpaRepo;

    @MockitoBean
    private DenomRepository productDenomRepository;

    @MockitoBean
    private StoreMutationRepositoryPort storeMutationRepository;

    @MockitoBean
    private BalanceManagementUseCase balanceManagementUseCase;

    @MockitoBean
    private ProviderPort providerService;

    @MockitoBean
    private KeycloakIdentityPort keycloakIdentityPort;

    @MockitoBean
    private KeycloakOrganizationPort keycloakOrganizationPort;

    @MockitoBean
    private WalletCreationPort walletCreationPort;

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
                new BigDecimal("10000.00"), BigDecimal.ZERO, true, false);

        // BalanceManagementUseCase mock — TransactionDomainService uses this for balance operations
        MutationResult debitResult = new MutationResult(UUID.randomUUID(), new BigDecimal("90000.00"));
        MutationResult refundResult = new MutationResult(UUID.randomUUID(), new BigDecimal("100000.00"));
        doReturn(debitResult).when(balanceManagementUseCase).deductBalance(any(), any(), any(), any(), any());
        doReturn(refundResult).when(balanceManagementUseCase).addBalance(any(), any(), any(), any(), any());
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

        // Mutation save: return stub with UUID
        when(storeMutationRepository.save(any(StoreMutations.class))).thenAnswer(invocation -> {
            StoreMutations mutation = invocation.getArgument(0);
            if (mutation.getId() == null) {
                mutation.setId(UUID.randomUUID());
            }
            return mutation;
        });
    }

    // ==============================================
    // SKENARIO 1: Happy Path — SUCCESS
    // ==============================================

    @Test
    void whenPurchase_withSufficientBalance_andProviderSuccess_thenTransactionSuccess() throws Exception {
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(true, "REF-123", "SN-123", "Success"));

        PurchaseRequest request = new PurchaseRequest(denomId, "081234567890");

        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TransactionStatus.SUCCESS.name()))
                .andExpect(jsonPath("$.providerRef").value("REF-123"))
                .andExpect(jsonPath("$.serialNumber").value("SN-123"));

        verify(providerService, times(1))
                .sendTransaction(eq("081234567890"), eq("PULSA10"), eq(new BigDecimal("10000.00")));
    }

    // ==============================================
    // SKENARIO 2: Saldo Tidak Cukup — HTTP 422
    // ==============================================

    @Test
    void whenPurchase_withInsufficientBalance_thenReturn422AndProviderNotCalled() throws Exception {
        when(balanceManagementUseCase.deductBalance(any(), any(), any(), any(), any()))
                .thenThrow(new InsufficientBalanceException("Saldo tidak mencukupi"));

        PurchaseRequest request = new PurchaseRequest(denomId, "081234567890");

        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                .andExpect(jsonPath("$.message").exists());

        verify(providerService, never())
                .sendTransaction(anyString(), anyString(), any(BigDecimal.class));
    }

    // ==============================================
    // SKENARIO 3: Provider Gagal — REFUNDED
    // ==============================================

    @Test
    void whenPurchase_withProviderFailure_thenTransactionRefunded() throws Exception {
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(false, null, null, "Timeout Biller"));

        PurchaseRequest request = new PurchaseRequest(denomId, "081234567890");

        mockMvc.perform(post("/api/transactions/purchase")
                .with(jwt())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(TransactionStatus.REFUNDED.name()));

        verify(providerService, times(1))
                .sendTransaction(eq("081234567890"), eq("PULSA10"), eq(new BigDecimal("10000.00")));

        // deductBalance (purchase) + addBalance (refund) both called
        verify(balanceManagementUseCase, times(1)).deductBalance(eq(walletId), any(), any(), any(), any());
        verify(balanceManagementUseCase, times(1)).addBalance(eq(walletId), any(), any(), any(), any());
    }
}
