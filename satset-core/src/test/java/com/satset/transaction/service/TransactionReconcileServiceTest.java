package com.satset.transaction.service;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.model.*;
import com.satset.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    TransactionReconcileService reconcile;

    UUID denomId = UUID.randomUUID();
    DenomInfo denom = new DenomInfo(denomId, "xld25", "XL 25K", "XL",
            new BigDecimal("25000.00"), BigDecimal.ZERO, new BigDecimal("24500.00"), true, false);

    @BeforeEach
    void setUp() {
        reconcile = new TransactionReconcileService(txRepo, denomRepo, provider, txService, 120000L, 100);
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
}
