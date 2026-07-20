package com.satset.webhook.service;

import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.ProviderStatus;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.TransactionDomainService;
import com.satset.webhook.dto.DigiflazzWebhookPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigiflazzWebhookServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private DenomRepository denomRepository;
    @Mock
    private TransactionDomainService transactionDomainService;

    private DigiflazzWebhookService service;

    private UUID denomId;
    private DenomInfo denom;

    @BeforeEach
    void setUp() {
        service = new DigiflazzWebhookService(transactionRepository, denomRepository, transactionDomainService);
        denomId = UUID.randomUUID();
        denom = new DenomInfo(denomId, "TLKM5", "Telkomsel 5K", "Telkomsel",
                new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("4600.00"), true, false);
    }

    @Test
    void handle_ProcessingTransaction_ReconcilesAndReturnsSettled() {
        Transactions tx = buildTransaction(TransactionStatus.PROCESSING, denomId);
        when(transactionRepository.findByRefNo("30467470")).thenReturn(Optional.of(tx));
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));

        DigiflazzWebhookPayload.Data data = data("30467470", "Sukses", "00", "SN-1", "199800");

        DigiflazzWebhookService.HandleResult result = service.handle(data);

        assertThat(result).isEqualTo(DigiflazzWebhookService.HandleResult.SETTLED);
        verify(transactionDomainService).reconcileProviderResult(
                eq(tx),
                argThatProviderResponse(ProviderStatus.SUCCESS, "30467470", "SN-1"),
                eq(tx.getWalletId()),
                eq(denom));
    }

    @Test
    void handle_UnknownRefNo_ThrowsResourceNotFound_ReconcileNeverCalled() {
        when(transactionRepository.findByRefNo("unknown-ref")).thenReturn(Optional.empty());

        DigiflazzWebhookPayload.Data data = data("unknown-ref", "Sukses", "00", "SN-1", "199800");

        assertThatThrownBy(() -> service.handle(data)).isInstanceOf(ResourceNotFoundException.class);
        verify(transactionDomainService, never()).reconcileProviderResult(any(), any(), any(), any());
    }

    @Test
    void handle_AlreadyTerminalTransaction_ReturnsReplayIgnored_ReconcileNeverCalled() {
        Transactions tx = buildTransaction(TransactionStatus.SUCCESS, denomId);
        when(transactionRepository.findByRefNo("30467470")).thenReturn(Optional.of(tx));

        DigiflazzWebhookPayload.Data data = data("30467470", "Gagal", "02", null, "0");

        DigiflazzWebhookService.HandleResult result = service.handle(data);

        assertThat(result).isEqualTo(DigiflazzWebhookService.HandleResult.REPLAY_IGNORED);
        verify(transactionDomainService, never()).reconcileProviderResult(any(), any(), any(), any());
    }

    @Test
    void handle_ReconcileThrows_Propagates() {
        Transactions tx = buildTransaction(TransactionStatus.PROCESSING, denomId);
        when(transactionRepository.findByRefNo("30467470")).thenReturn(Optional.of(tx));
        when(denomRepository.findDenomInfoById(denomId)).thenReturn(Optional.of(denom));
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(transactionDomainService).reconcileProviderResult(any(), any(), any(), any());

        DigiflazzWebhookPayload.Data data = data("30467470", "Sukses", "00", "SN-1", "199800");

        assertThatThrownBy(() -> service.handle(data)).isInstanceOf(RuntimeException.class).hasMessage("db down");
    }

    private static Transactions buildTransaction(TransactionStatus status, UUID denomId) {
        Transactions tx = new Transactions();
        tx.setId(UUID.randomUUID());
        tx.setRefNo("30467470");
        tx.setWalletId("7001234567");
        tx.setProductDenomId(denomId);
        tx.setStatus(status);
        return tx;
    }

    private static DigiflazzWebhookPayload.Data data(String refId, String status, String rc, String sn, String price) {
        return new DigiflazzWebhookPayload.Data(refId, "0812", "TLKM5", status, status, rc, sn, new BigDecimal(price));
    }

    private static ProviderResponse argThatProviderResponse(ProviderStatus status, String refId, String sn) {
        return org.mockito.ArgumentMatchers.argThat(r ->
                r.status() == status && r.referenceNumber().equals(refId) && sn.equals(r.serialNumber()));
    }
}
