package com.omnip.transaction.domain.service;

import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.shared.exception.InsufficientBalanceException;
import com.omnip.transaction.domain.port.out.TransactionRepositoryPort;
import com.omnip.transaction.domain.port.out.StoreBalancePort;
import com.omnip.transaction.domain.model.MutationReferenceType;
import com.omnip.transaction.domain.model.ProviderResponse;
import com.omnip.transaction.domain.model.TransactionSummary;
import com.omnip.transaction.domain.model.Transactions;
import com.omnip.transaction.domain.model.TransactionStatus;
import com.omnip.transaction.domain.port.out.ProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionDomainServiceTest {

    @Mock
    private TransactionRepositoryPort transactionRepository;
    @Mock
    private StoreBalancePort storeRepository;
    @Mock
    private DenomRepositoryPort productDenomRepository;
    @Mock
    private BalanceDomainService balanceService;
    @Mock
    private ProviderPort providerService;

    @InjectMocks
    private TransactionDomainService transactionService;

    private UUID storeId;
    private UUID denomId;
    private Stores store;
    private ProductDenoms denom;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        denomId = UUID.randomUUID();

        store = new Stores();
        store.setId(storeId);
        store.setBalance(new BigDecimal("10000.00"));

        denom = new ProductDenoms();
        denom.setId(denomId);
        denom.setCode("TLKM5");
        denom.setName("Telkomsel 5K");
        denom.setPrice(new BigDecimal("5000.00"));
        denom.setAdminFee(BigDecimal.ZERO);
        denom.setActive(true);
    }

    @Test
    void createPurchase_Success_WhenBalanceIsExact() throws InsufficientBalanceException {
        // "Saldo pas-pasan → SUCCESS, balance = 0"
        denom.setPrice(new BigDecimal("10000.00")); // Pas dengan saldo store

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productDenomRepository.findById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(true, "REF-123", "SN-123", "Success"));

        TransactionSummary result = transactionService.createPurchase(storeId, denomId, "081234567890");

        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.status());
        assertEquals(new BigDecimal("10000.00"), result.total());

        verify(balanceService, times(1)).deductBalance(eq(storeId), eq(new BigDecimal("10000.00")),
                eq(MutationReferenceType.PURCHASE), any(UUID.class), anyString());
        verify(providerService, times(1)).sendTransaction("081234567890", "TLKM5", new BigDecimal("10000.00"));
    }

    @Test
    void createPurchase_ThrowsException_WhenBalanceInsufficient() throws InsufficientBalanceException {
        // "Saldo kurang Rp 1 → REJECTED, balance unchanged"
        denom.setPrice(new BigDecimal("10001.00")); // Kurang Rp 1

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productDenomRepository.findById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        doThrow(new InsufficientBalanceException("Saldo tidak mencukupi"))
                .when(balanceService).deductBalance(eq(storeId), eq(new BigDecimal("10001.00")),
                        eq(MutationReferenceType.PURCHASE), any(UUID.class), anyString());

        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.createPurchase(storeId, denomId, "081234567890"));

        // Verify provider is never called
        verify(providerService, never()).sendTransaction(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void createPurchase_ProviderFailed_RefundsBalance() throws InsufficientBalanceException {
        // "Provider timeout → FAILED → auto refund"
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productDenomRepository.findById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        // Provider fails
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(false, null, null, "Timeout API"));

        TransactionSummary result = transactionService.createPurchase(storeId, denomId, "081234567890");

        assertNotNull(result);
        assertEquals(TransactionStatus.REFUNDED, result.status());

        // Verify balance was deducted initially
        verify(balanceService, times(1)).deductBalance(eq(storeId), eq(new BigDecimal("5000.00")),
                eq(MutationReferenceType.PURCHASE), any(UUID.class), anyString());

        // Verify balance was added back (refunded)
        verify(balanceService, times(1)).addBalance(eq(storeId), eq(new BigDecimal("5000.00")),
                eq(MutationReferenceType.REFUND), eq(result.id()), anyString());
    }

    @Test
    void createPurchase_DenomInactive_ThrowsException() throws InsufficientBalanceException {
        // "Purchase denom inactive/deleted → REJECTED"
        // Wait, the code doesn't check if denom is active right now.
        // Let's add the test to expect Business rule exception. The service must be
        // modified to satisfy this!
        denom.setActive(false);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productDenomRepository.findById(denomId)).thenReturn(Optional.of(denom));

        // This will currently fail because the service doesn't check this. We'll update
        // code soon.
        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createPurchase(storeId, denomId, "081234567890"));

        verify(transactionRepository, never()).save(any());
        verify(balanceService, never()).deductBalance(any(), any(), any(), any(), any());
    }

    @Test
    void createPurchase_DoubleSubmit_ThrowsException() throws InsufficientBalanceException {
        // "Double submit (idempotency) → second rejected"
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productDenomRepository.findById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                eq(storeId), eq(denomId), eq("081234567890"), any(), any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createPurchase(storeId, denomId, "081234567890"));

        verify(transactionRepository, never()).save(any());
        verify(balanceService, never()).deductBalance(any(), any(), any(), any(), any());
    }

    @Test
    void createPurchase_RefundFails_LeavesStatusFailed() throws InsufficientBalanceException {
        // "Refund gagal setelah provider fail → alert"
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productDenomRepository.findById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(false, null, null, "Timeout API"));

        doThrow(new RuntimeException("Database error during refund"))
                .when(balanceService).addBalance(any(), any(), any(), any(), any());

        TransactionSummary result = transactionService.createPurchase(storeId, denomId, "081234567890");

        assertNotNull(result);
        assertEquals(TransactionStatus.FAILED, result.status()); // Does not become REFUNDED
    }

    @Test
    void createPurchase_ConcurrentPurchases_OnlyOneSucceeds() throws InsufficientBalanceException {
        // "Concurrent purchase (2 thread, 1 saldo) → hanya 1 berhasil"
        // Simulasi race condition dengan mock: call 1 berhasil, call 2 throw
        // InsufficientBalanceException
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productDenomRepository.findById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                // Return different IDs for the two simulated calls
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        // Setup balanceService to succeed once, then fail
        when(balanceService.deductBalance(any(), any(), any(), any(), any()))
                .thenReturn(new com.omnip.transaction.domain.model.StoreMutations())
                .thenThrow(new InsufficientBalanceException("Saldo tidak mencukupi"));

        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(true, "REF-123", "SN-123", "Success"));

        // Call 1 - Succeeds
        TransactionSummary tx1 = transactionService.createPurchase(storeId, denomId, "081234567890");
        assertEquals(TransactionStatus.SUCCESS, tx1.status());

        // Call 2 - Fails because balance service throws error on pseudo-concurrent
        // request
        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.createPurchase(storeId, denomId, "081234567891") // Different number so
                                                                                          // idempotency doesn't block
                                                                                          // it
        );
    }

}
