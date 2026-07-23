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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    long staleAfterMs = 120_000L;
    int batchSize = 100;
    long maxAgeMs = 21_600_000L; // 6h

    @BeforeEach
    void setUp() {
        // run the TransactionTemplate callback inline
        lenient().doAnswer(inv -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> c = inv.getArgument(0);
            c.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        reconcile = new TransactionReconcileService(
                txRepo, denomRepo, provider, txService, transactionTemplate,
                staleAfterMs, batchSize, maxAgeMs);
    }

    private Transactions stale() {
        Transactions tx = new Transactions();
        tx.setId(UUID.randomUUID());
        tx.setWalletId("w1");
        tx.setProductDenomId(denomId);
        tx.setTargetNumber("0878");
        tx.setTotal(new BigDecimal("25000.00"));
        tx.setStatus(TransactionStatus.PROCESSING);
        return tx; // createdAt null → age check skipped
    }

    private ProviderResponse ok() {
        return new ProviderResponse(ProviderStatus.SUCCESS, "REF", "SN", "Sukses", new BigDecimal("24500"));
    }

    @Test
    void repollsStale_andSettles() {
        Transactions tx = stale();
        when(txRepo.findByStatusAndCreatedAtBetween(eq(TransactionStatus.PROCESSING), any(), any(), any()))
                .thenReturn(List.of(tx));
        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        ProviderResponse resp = ok();
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx.getId().toString()))
                .thenReturn(resp);

        reconcile.reconcileStalePending();

        verify(provider).sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx.getId().toString());
        verify(txService).reconcileProviderResult(tx, resp, "w1", denom);
    }

    @Test
    void usesStoredRefNo_notUuid() {
        Transactions tx = stale();
        tx.setRefNo("2026072300042");
        when(txRepo.findByStatusAndCreatedAtBetween(eq(TransactionStatus.PROCESSING), any(), any(), any()))
                .thenReturn(List.of(tx));
        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), "2026072300042"))
                .thenReturn(ok());

        reconcile.reconcileStalePending();

        verify(provider).sendTransaction("0878", "xld25", new BigDecimal("25000.00"), "2026072300042");
    }

    @Test
    void empty_doesNothing() {
        when(txRepo.findByStatusAndCreatedAtBetween(eq(TransactionStatus.PROCESSING), any(), any(), any()))
                .thenReturn(List.of());

        reconcile.reconcileStalePending();

        verifyNoInteractions(provider, txService);
    }

    @Test
    void oneRowThrows_doesNotPoisonOtherRows() {
        Transactions tx1 = stale();
        Transactions tx2 = stale();
        when(txRepo.findByStatusAndCreatedAtBetween(eq(TransactionStatus.PROCESSING), any(), any(), any()))
                .thenReturn(List.of(tx1, tx2));
        when(txRepo.findById(tx1.getId())).thenReturn(Optional.of(tx1));
        when(txRepo.findById(tx2.getId())).thenReturn(Optional.of(tx2));
        when(denomRepo.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx1.getId().toString()))
                .thenThrow(new RuntimeException("boom"));
        ProviderResponse resp2 = ok();
        when(provider.sendTransaction("0878", "xld25", new BigDecimal("25000.00"), tx2.getId().toString()))
                .thenReturn(resp2);

        reconcile.reconcileStalePending();

        verify(txService, never()).reconcileProviderResult(eq(tx1), any(), any(), any());
        verify(txService).reconcileProviderResult(tx2, resp2, "w1", denom);
    }

    @Test
    void reFetchGuard_skipsRowNoLongerProcessing() {
        Transactions tx = stale();
        Transactions settled = stale();
        settled.setId(tx.getId());
        settled.setStatus(TransactionStatus.SUCCESS); // webhook settled between scan and settle
        when(txRepo.findByStatusAndCreatedAtBetween(eq(TransactionStatus.PROCESSING), any(), any(), any()))
                .thenReturn(List.of(tx));
        when(txRepo.findById(tx.getId())).thenReturn(Optional.of(settled));

        reconcile.reconcileStalePending();

        verifyNoInteractions(provider, txService);
    }

    @Test
    void stuckRowsPastMaxAge_countedForAlert_notInReconcileBatch() {
        when(txRepo.countByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any())).thenReturn(3L);
        when(txRepo.findByStatusAndCreatedAtBetween(eq(TransactionStatus.PROCESSING), any(), any(), any()))
                .thenReturn(List.of()); // give-up rows excluded by the finder's lower bound
        reconcile.reconcileStalePending();
        verify(txRepo).countByStatusAndCreatedAtBefore(eq(TransactionStatus.PROCESSING), any());
        verifyNoInteractions(provider, txService); // stuck rows never re-polled
    }

    @Test
    void scan_usesBatchSizeAndOldestFirst_andWindowBoundsInOrder() {
        when(txRepo.findByStatusAndCreatedAtBetween(eq(TransactionStatus.PROCESSING), any(), any(), any()))
                .thenReturn(List.of());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);

        reconcile.reconcileStalePending();

        verify(txRepo).findByStatusAndCreatedAtBetween(
                eq(TransactionStatus.PROCESSING), from.capture(), to.capture(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(batchSize);
        assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isAscending()).isTrue();
        // lower bound (maxCutoff) must be chronologically before upper bound (staleCutoff),
        // else Between inverts and the scan silently returns nothing
        assertThat(from.getValue()).isBefore(to.getValue());
    }
}
