package com.omnip.transaction.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnip.catalog.adapter.out.persistence.DenomJpaRepository;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import com.omnip.onboarding.adapter.out.persistence.StoreJpaRepository;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.shared.dto.UserDTO;
import com.omnip.transaction.adapter.in.web.dto.PurchaseRequest;
import com.omnip.transaction.adapter.out.persistence.StoreMutationJpaRepository;
import com.omnip.transaction.adapter.out.persistence.TransactionJpaRepository;
import com.omnip.transaction.domain.model.ProviderResponse;
import com.omnip.transaction.domain.model.StoreMutations;
import com.omnip.transaction.domain.model.TransactionStatus;
import com.omnip.transaction.domain.model.Transactions;
import com.omnip.transaction.domain.port.out.ProviderPort;
import com.omnip.transaction.domain.port.out.StoreBalancePort;
import com.omnip.transaction.domain.port.out.StoreMutationRepositoryPort;
import com.omnip.transaction.domain.port.out.TransactionRepositoryPort;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PurchaseFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // @MockitoBean on JPA repo types to cover all implemented interfaces (port + CrudRepository).
    // This prevents Spring Data from creating real proxies and satisfies all injection points.
    @MockitoBean
    private TransactionJpaRepository transactionJpaRepo;

    @MockitoBean
    private StoreJpaRepository storeJpaRepo;

    @MockitoBean
    private DenomJpaRepository denomJpaRepo;

    @MockitoBean
    private StoreMutationJpaRepository mutationJpaRepo;

    @MockitoBean
    private ProviderPort providerService;

    @MockitoBean
    private UserDTO userDTO;

    // Port-typed aliases to avoid method ambiguity (JPA repos inherit conflicting
    // signatures from CrudRepository and port interfaces). Assigned in setUp().
    private TransactionRepositoryPort transactionRepository;
    private StoreBalancePort storeRepository;
    private DenomRepositoryPort productDenomRepository;
    private StoreMutationRepositoryPort storeMutationRepository;

    private UUID storeId;
    private UUID denomId;
    private Stores store;
    private ProductDenoms denom;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Assign port-typed aliases (same mock objects, narrower type avoids ambiguity)
        transactionRepository = transactionJpaRepo;
        storeRepository = storeJpaRepo;
        productDenomRepository = denomJpaRepo;
        storeMutationRepository = mutationJpaRepo;

        storeId = UUID.randomUUID();
        denomId = UUID.randomUUID();

        // Setup store with sufficient balance
        store = new Stores();
        store.setId(storeId);
        store.setBalance(new BigDecimal("100000.00"));

        // Setup product denom
        denom = new ProductDenoms();
        denom.setId(denomId);
        denom.setCode("PULSA10");
        denom.setName("Telkomsel 10K");
        denom.setPrice(new BigDecimal("10000.00"));
        denom.setAdminFee(BigDecimal.ZERO);
        denom.setActive(true);
        denom.setDeleted(false);

        // UserDTO provides store context to controller
        when(userDTO.getStoreId()).thenReturn(storeId);

        // Store lookup
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.of(store));
        when(storeRepository.save(any(Stores.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Denom lookup
        when(productDenomRepository.findById(denomId)).thenReturn(Optional.of(denom));

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
        // Store hanya punya 5000, harga denom 10000
        store.setBalance(new BigDecimal("5000.00"));
        when(storeRepository.findByIdWithPessimisticLock(storeId)).thenReturn(Optional.of(store));

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

        // BalanceDomainService: findByIdWithPessimisticLock dipanggil 2x (deduct + refund)
        verify(storeRepository, atLeast(2)).findByIdWithPessimisticLock(storeId);
    }
}
