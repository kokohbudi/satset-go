package com.satset.transaction.service;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.dto.TransactionDTO;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.client.WalletClientAdapter;
import com.satset.transaction.model.*;
import com.satset.transaction.client.ProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionDomainServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private DenomRepository denomRepository;
    @Mock
    private WalletClientAdapter balanceService;
    @Mock
    private ProviderPort providerService;

    @InjectMocks
    private TransactionDomainService transactionService;

    private UUID storeId;
    private String walletId;
    private UUID denomId;
    private DenomInfo denom;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        walletId = "7001234567";
        denomId = UUID.randomUUID();

        denom = new DenomInfo(
            denomId,
            "TLKM5",
            "Telkomsel 5K",
            "Telkomsel",
            new BigDecimal("5000.00"),
            BigDecimal.ZERO,
            true,
            false
        );
    }

    @Test
    void createPurchase_Success_WhenBalanceIsExact() throws InsufficientBalanceException {
        // "Saldo pas-pasan → SUCCESS, balance = 0"
        DenomInfo expensiveDenom = new DenomInfo(
            denomId, "TLKM10", "Telkomsel 10K", "Telkomsel",
            new BigDecimal("10000.00"), BigDecimal.ZERO, true, false
        );

        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(expensiveDenom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(true, "REF-123", "SN-123", "Success"));

        TransactionDTO result = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.status());
        assertEquals(new BigDecimal("10000.00"), result.total());

        verify(balanceService, times(1)).deductBalance(eq(walletId), eq(new BigDecimal("10000.00")),
                any(UUID.class), anyString());
        verify(providerService, times(1)).sendTransaction("081234567890", "TLKM10", new BigDecimal("10000.00"));
    }

    @Test
    void createPurchase_ThrowsException_WhenBalanceInsufficient() throws InsufficientBalanceException {
        // "Saldo kurang Rp 1 → REJECTED, balance unchanged"
        DenomInfo expensiveDenom = new DenomInfo(
            denomId, "TLKM10", "Telkomsel 10K", "Telkomsel",
            new BigDecimal("10001.00"), BigDecimal.ZERO, true, false
        );

        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(expensiveDenom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        doThrow(new InsufficientBalanceException("Saldo tidak mencukupi"))
                .when(balanceService).deductBalance(eq(walletId), eq(new BigDecimal("10001.00")),
                        any(UUID.class), anyString());

        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.createPurchase(storeId, walletId, denomId, "081234567890"));

        // Verify provider is never called
        verify(providerService, never()).sendTransaction(anyString(), anyString(), any(BigDecimal.class));
    }

    @Test
    void createPurchase_ProviderFailed_RefundsBalance() throws InsufficientBalanceException {
        // "Provider timeout → FAILED → auto refund"
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
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

        TransactionDTO result = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        assertNotNull(result);
        assertEquals(TransactionStatus.REFUNDED, result.status());

        // Verify balance was deducted initially
        verify(balanceService, times(1)).deductBalance(eq(walletId), eq(new BigDecimal("5000.00")),
                any(UUID.class), anyString());

        // Verify balance was added back (refunded)
        verify(balanceService, times(1)).addBalance(eq(walletId), eq(new BigDecimal("5000.00")),
                eq(MutationReferenceType.REFUND), eq(result.id()), anyString());
    }

    @Test
    void createPurchase_DenomInactive_ThrowsException() throws InsufficientBalanceException {
        // "Purchase denom inactive/deleted → REJECTED"
        DenomInfo inactiveDenom = new DenomInfo(
            denomId, "TLKM5", "Telkomsel 5K", "Telkomsel",
            new BigDecimal("5000.00"), BigDecimal.ZERO, false, false
        );
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(inactiveDenom));

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createPurchase(storeId, walletId, denomId, "081234567890"));

        verify(transactionRepository, never()).save(any());
        verify(balanceService, never()).deductBalance(any(), any(), any(), any());
    }

    @Test
    void createPurchase_DoubleSubmit_ThrowsException() throws InsufficientBalanceException {
        // "Double submit (idempotency) → second rejected"
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                eq(storeId), eq(denomId), eq("081234567890"), any(), any())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createPurchase(storeId, walletId, denomId, "081234567890"));

        verify(transactionRepository, never()).save(any());
        verify(balanceService, never()).deductBalance(any(), any(), any(), any());
    }

    @Test
    void createPurchase_RefundFails_LeavesStatusFailed() throws InsufficientBalanceException {
        // "Refund gagal setelah provider fail → alert"
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
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
                .when(balanceService).addBalance(any(String.class), any(BigDecimal.class),
                eq(MutationReferenceType.REFUND), any(UUID.class), anyString());

        TransactionDTO result = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        assertNotNull(result);
        assertEquals(TransactionStatus.FAILED, result.status()); // Does not become REFUNDED
    }


    @Test
    void createPurchase_DenomNotFound_ThrowsException() {
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.empty());

        assertThrows(com.satset.shared.exception.ResourceNotFoundException.class,
                () -> transactionService.createPurchase(storeId, walletId, denomId, "081234567890"));

        verify(transactionRepository, never()).save(any());
    }

    // ==================== topUp ====================

    @Test
    void topUp_Success_CallsAddBalance() {
        transactionService.topUp(walletId, new BigDecimal("50000"), "Isi saldo");

        verify(balanceService).addBalance(eq(walletId), eq(new BigDecimal("50000")),
                eq(MutationReferenceType.TOP_UP), any(UUID.class), eq("Isi saldo"));
    }

    @Test
    void topUp_NullDescription_UsesDefaultDescription() {
        transactionService.topUp(walletId, new BigDecimal("50000"), null);

        verify(balanceService).addBalance(eq(walletId), eq(new BigDecimal("50000")),
                eq(MutationReferenceType.TOP_UP), any(UUID.class), eq("Manual top-up"));
    }


    // ==================== getTransaction ====================

    @Test
    void getTransaction_Found_ReturnsSummary() {
        UUID txId = UUID.randomUUID();
        Transactions tx = buildTransaction(txId);
        when(transactionRepository.findByIdAndStoreId(txId, storeId))
                .thenReturn(Optional.of(tx));

        TransactionDTO result = transactionService.getTransaction(txId, storeId);

        assertNotNull(result);
        assertEquals(txId, result.id());
        assertEquals(TransactionStatus.SUCCESS, result.status());
    }

    @Test
    void getTransaction_NotFound_ThrowsException() {
        UUID txId = UUID.randomUUID();
        when(transactionRepository.findByIdAndStoreId(txId, storeId))
                .thenReturn(Optional.empty());

        assertThrows(com.satset.shared.exception.ResourceNotFoundException.class,
                () -> transactionService.getTransaction(txId, storeId));
    }

    // ==================== getTransactionHistory ====================

    @Test
    void getTransactionHistory_ReturnsPage() {
        UUID txId = UUID.randomUUID();
        Transactions tx = buildTransaction(txId);
        org.springframework.data.domain.Page<Transactions> page =
                new org.springframework.data.domain.PageImpl<>(List.of(tx));
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);
        when(transactionRepository.findByStoreId(storeId, pageable)).thenReturn(page);

        org.springframework.data.domain.Page<TransactionDTO> result =
                transactionService.getTransactionHistory(storeId, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(txId, result.getContent().get(0).id());
    }


    // ==================== Helpers ====================

    private Transactions buildTransaction(UUID txId) {
        Transactions tx = new Transactions();
        tx.setId(txId);
        tx.setStoreId(storeId);
        tx.setProductDenomId(denomId);
        tx.setDenomName("Telkomsel 5K");
        tx.setProductName("Telkomsel");
        tx.setTargetNumber("081234567890");
        tx.setPrice(new BigDecimal("5000.00"));
        tx.setAdminFee(BigDecimal.ZERO);
        tx.setTotal(new BigDecimal("5000.00"));
        tx.setStatus(TransactionStatus.SUCCESS);
        return tx;
    }

    @Test
    void createPurchase_ConcurrentPurchases_OnlyOneSucceeds() throws InsufficientBalanceException {
        // "Concurrent purchase (2 thread, 1 saldo) → hanya 1 berhasil"
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        // Setup balanceService to succeed once, then fail
        doNothing()
                .doThrow(new InsufficientBalanceException("Saldo tidak mencukupi"))
                .when(balanceService).deductBalance(any(), any(), any(), any());

        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(new ProviderResponse(true, "REF-123", "SN-123", "Success"));

        // Call 1 - Succeeds
        TransactionDTO tx1 = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");
        assertEquals(TransactionStatus.SUCCESS, tx1.status());

        // Call 2 - Fails because balance service throws error on pseudo-concurrent request
        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.createPurchase(storeId, walletId, denomId, "081234567891"));
    }

}
