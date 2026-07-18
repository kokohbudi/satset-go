package com.satset.transaction.service.topup;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.InsufficientBalanceException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.dto.TransactionDTO;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.model.*;
import com.satset.transaction.client.ProviderPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private WalletGateway balanceService;
    @Mock
    private ProviderPort providerService;
    @Mock
    private RefNoGenerator refNoGenerator;

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
        // lenient: duplicate-check tests throw before ref_no is generated
        lenient().when(refNoGenerator.next()).thenReturn("20260718000001");

        denom = new DenomInfo(
            denomId,
            "TLKM5",
            "Telkomsel 5K",
            "Telkomsel",
            new BigDecimal("5000.00"),
            BigDecimal.ZERO,
            new BigDecimal("4600.00"),
            true,
            false
        );
    }

    @Test
    void createPurchase_Success_WhenBalanceIsExact() throws InsufficientBalanceException {
        // "Saldo pas-pasan → SUCCESS, balance = 0"
        DenomInfo expensiveDenom = new DenomInfo(
            denomId, "TLKM10", "Telkomsel 10K", "Telkomsel",
            new BigDecimal("10000.00"), BigDecimal.ZERO, new BigDecimal("9000.00"), true, false
        );

        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(expensiveDenom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(invocation -> {
            Transactions tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.SUCCESS, "REF-123", "SN-123", "Success", null));

        TransactionDTO result = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        assertNotNull(result);
        assertEquals(TransactionStatus.SUCCESS, result.status());
        assertEquals(new BigDecimal("10000.00"), result.total());

        verify(balanceService, times(1)).deductBalance(eq(walletId), eq(new BigDecimal("10000.00")),
                any(UUID.class), anyString());
        verify(providerService, times(1)).sendTransaction(eq("081234567890"), eq("TLKM10"), eq(new BigDecimal("10000.00")), anyString());
    }

    @Test
    void createPurchase_ThrowsException_WhenBalanceInsufficient() throws InsufficientBalanceException {
        // "Saldo kurang Rp 1 → REJECTED, balance unchanged"
        DenomInfo expensiveDenom = new DenomInfo(
            denomId, "TLKM10", "Telkomsel 10K", "Telkomsel",
            new BigDecimal("10001.00"), BigDecimal.ZERO, new BigDecimal("9000.00"), true, false
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
        verify(providerService, never()).sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString());
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
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.FAILED, null, null, "Timeout API", null));

        TransactionDTO result = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        assertNotNull(result);
        assertEquals(TransactionStatus.REFUNDED, result.status());

        // Verify balance was deducted initially
        verify(balanceService, times(1)).deductBalance(eq(walletId), eq(new BigDecimal("5000.00")),
                any(UUID.class), anyString());

        // Verify balance was added back (refunded)
        verify(balanceService, times(1)).refundBalance(eq(walletId), eq(new BigDecimal("5000.00")),
                eq(result.id()), anyString());
    }

    @Test
    void createPurchase_Success_SnapshotsMargin_FallbackToBasePrice() throws InsufficientBalanceException {
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom)); // basePrice 4600, total 5000
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
            Transactions tx = inv.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            return tx;
        });
        // provider reports no cost -> fallback to basePrice
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.SUCCESS, "REF-1", "SN-1", "OK", null));

        transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
        verify(transactionRepository, atLeastOnce()).save(captor.capture());
        Transactions saved = captor.getValue(); // last save = SUCCESS state
        assertEquals(new BigDecimal("4600.00"), saved.getCostPrice());
        assertEquals(new BigDecimal("400.00"), saved.getMargin());
    }

    @Test
    void createPurchase_Success_ProviderCostOverridesBasePrice() throws InsufficientBalanceException {
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
            Transactions tx = inv.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            return tx;
        });
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.SUCCESS, "REF-2", "SN-2", "OK", new BigDecimal("4800.00")));

        transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
        verify(transactionRepository, atLeastOnce()).save(captor.capture());
        Transactions saved = captor.getValue();
        assertEquals(new BigDecimal("4800.00"), saved.getCostPrice());
        assertEquals(new BigDecimal("200.00"), saved.getMargin());
    }

    @Test
    void createPurchase_Failed_LeavesCostAndMarginNull() throws InsufficientBalanceException {
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
            Transactions tx = inv.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            return tx;
        });
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.FAILED, null, null, "Timeout", null));

        transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
        verify(transactionRepository, atLeastOnce()).save(captor.capture());
        Transactions saved = captor.getValue();
        assertNull(saved.getCostPrice());
        assertNull(saved.getMargin());
    }

    @Test
    void createPurchase_DenomInactive_ThrowsException() throws InsufficientBalanceException {
        // "Purchase denom inactive/deleted → REJECTED"
        DenomInfo inactiveDenom = new DenomInfo(
            denomId, "TLKM5", "Telkomsel 5K", "Telkomsel",
            new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("4600.00"), false, false
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

        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.FAILED, null, null, "Timeout API", null));

        doThrow(new RuntimeException("Database error during refund"))
                .when(balanceService).refundBalance(any(String.class), any(BigDecimal.class),
                any(UUID.class), anyString());

        TransactionDTO result = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        assertNotNull(result);
        assertEquals(TransactionStatus.FAILED, result.status()); // Does not become REFUNDED
    }


    @Test
    void createPurchase_Pending_StaysProcessing_NoRefund() throws InsufficientBalanceException {
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
            Transactions tx = inv.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            return tx;
        });
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.PENDING, "REF-P", null, "Transaksi Pending", null));

        TransactionDTO result = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        assertEquals(TransactionStatus.PROCESSING, result.status());
        assertEquals("REF-P", result.providerRef());
        verify(balanceService, never()).refundBalance(any(), any(), any(), any());
    }

    @Test
    void createPurchase_DenomNotFound_ThrowsException() {
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.empty());

        assertThrows(com.satset.shared.exception.ResourceNotFoundException.class,
                () -> transactionService.createPurchase(storeId, walletId, denomId, "081234567890"));

        verify(transactionRepository, never()).save(any());
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
    void createPurchase_PersistsWalletId() throws InsufficientBalanceException {
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
            Transactions tx = inv.getArgument(0);
            if (tx.getId() == null) tx.setId(UUID.randomUUID());
            return tx;
        });
        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.SUCCESS, "REF-1", "SN-1", "OK", null));

        transactionService.createPurchase(storeId, walletId, denomId, "081234567890");

        ArgumentCaptor<Transactions> captor = ArgumentCaptor.forClass(Transactions.class);
        verify(transactionRepository, atLeastOnce()).save(captor.capture());
        assertEquals(walletId, captor.getValue().getWalletId());
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

        when(providerService.sendTransaction(anyString(), anyString(), any(BigDecimal.class), anyString()))
                .thenReturn(new ProviderResponse(ProviderStatus.SUCCESS, "REF-123", "SN-123", "Success", null));

        // Call 1 - Succeeds
        TransactionDTO tx1 = transactionService.createPurchase(storeId, walletId, denomId, "081234567890");
        assertEquals(TransactionStatus.SUCCESS, tx1.status());

        // Call 2 - Fails because balance service throws error on pseudo-concurrent request
        assertThrows(InsufficientBalanceException.class,
                () -> transactionService.createPurchase(storeId, walletId, denomId, "081234567891"));
    }

}
