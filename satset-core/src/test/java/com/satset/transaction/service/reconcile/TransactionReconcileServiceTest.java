package com.satset.transaction.service.reconcile;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.*;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionReconcileServiceTest {

    @Mock TransactionRepository txRepo;
    @Mock DenomRepository denomRepo;
    @Mock ProviderPort provider;
    @Mock TransactionDomainService txService;
    @Mock TransactionTemplate transactionTemplate;

    TransactionReconcileService reconcile;

    UUID denomId = UUID.randomUUID();
    DenomInfo denom = new DenomInfo(denomId, "xld25", "XL 25K", "XL",
            new BigDecimal("25000.00"), BigDecimal.ZERO, new BigDecimal("24500.00"), true, false);

    @BeforeEach
    void setUp() {
        // run the callback inline
        org.mockito.Mockito.lenient().doAnswer(inv -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> c = inv.getArgument(0);
            c.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        reconcile = new TransactionReconcileService(txRepo, denomRepo, provider, txService, transactionTemplate, 120000L, 100);
    }

    private Transactions stale() {
        Transactions tx = new Transactions();
        tx.setId(UUID.randomUUID());
        tx.setWalletId("w1");
        tx.setProductDenomId(denomId);
        tx.setTargetNumber("0878");
        tx.setTotal(new BigDecimal("25000.00"));
        tx.setStatus(TransactionStatus.PROCESSING);
        return tx;
    }

    @Test
    void reconcile_repollsStale_andSettles() {
        Transactions tx = stale();
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of(tx));
        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        ProviderResponse resp = new ProviderResponse(ProviderStatus.SUCCESS, "REF", "SN", "Sukses", new BigDecimal("24500"));
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx.getId().toString()))
                .thenReturn(resp);

        reconcile.reconcileStalePending();

        verify(provider).sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx.getId().toString());
        verify(txService).applyProviderResult(tx, resp, "w1", denom);
    }

    @Test
    void reconcile_empty_doesNothing() {
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of());

        reconcile.reconcileStalePending();

        verifyNoInteractions(provider, txService);
    }

    @Test
    void reconcile_oneRowThrows_doesNotPoisonOtherRows() {
        Transactions tx1 = stale();
        Transactions tx2 = stale();
        when(txRepo.findByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any(), any()))
                .thenReturn(List.of(tx1, tx2));
        when(txRepo.findById(tx1.getId())).thenReturn(Optional.of(tx1));
        when(txRepo.findById(tx2.getId())).thenReturn(Optional.of(tx2));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        ProviderResponse resp2 = new ProviderResponse(ProviderStatus.SUCCESS, "REF", "SN", "Sukses", new BigDecimal("24500"));
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx1.getId().toString()))
                .thenThrow(new RuntimeException("boom"));
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx2.getId().toString()))
                .thenReturn(resp2);

        reconcile.reconcileStalePending();

        verify(provider).sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx1.getId().toString());
        verify(provider).sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx2.getId().toString());
        verify(txService, never()).applyProviderResult(eq(tx1), any(), any(), any());
        verify(txService).applyProviderResult(tx2, resp2, "w1", denom);
    }
}
