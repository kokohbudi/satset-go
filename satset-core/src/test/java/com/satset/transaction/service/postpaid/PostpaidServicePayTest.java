package com.satset.transaction.service.postpaid;

import com.satset.catalog.model.DenomType;
import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.SupplierException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.TransactionDTO;
import com.satset.transaction.model.InquiryResult;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.ProviderStatus;
import com.satset.transaction.model.TransactionStatus;
import com.satset.transaction.model.Transactions;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.RefNoGenerator;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PostpaidServicePayTest {

    private static final UUID DENOM_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID TX_ID = UUID.randomUUID();

    private final DenomRepository denomRepository = mock(DenomRepository.class);
    private final WalletGateway walletGateway = mock(WalletGateway.class);
    private final ProviderPort providerPort = mock(ProviderPort.class);
    private final RefNoGenerator refNoGenerator = mock(RefNoGenerator.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final TransactionDomainService transactionDomainService = mock(TransactionDomainService.class);

    private final PostpaidService service = new PostpaidService(denomRepository, walletGateway,
            providerPort, refNoGenerator, transactionRepository, transactionDomainService);

    private static DenomInfo pascaDenom() {
        return new DenomInfo(DENOM_ID, "pln", "PLN Pascabayar", "PLN", BigDecimal.ZERO,
                new BigDecimal("1500"), BigDecimal.ZERO, true, false,
                true, DenomType.FIXED_DENOM, null, null);
    }

    @BeforeEach
    void baseStubs() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(pascaDenom()));
        when(transactionRepository.existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                eq(STORE_ID), eq(DENOM_ID), eq("530000000001"), anyList(), any(LocalDateTime.class)))
                .thenReturn(false);
        when(refNoGenerator.next()).thenReturn("TRX010");
        when(transactionRepository.save(any(Transactions.class))).thenAnswer(inv -> {
            Transactions t = inv.getArgument(0);
            t.setId(TX_ID);
            return t;
        });
    }

    private void stubFreshInquiry(String bill) {
        when(providerPort.inquiry("530000000001", "pln", "TRX010", null))
                .thenReturn(new InquiryResult("BUDI SANTOSO", new BigDecimal(bill),
                        new BigDecimal("2500"), "00", "Sukses", null));
    }

    @Test
    void successfulPayDeductsChargesAndReconciles() throws Exception {
        stubFreshInquiry("145000"); // total = 145000 + 2500 + 1500 = 149000
        ProviderResponse payResp = new ProviderResponse(ProviderStatus.SUCCESS, "DF123",
                "STRUK/PLN/1234567890", "Sukses", new BigDecimal("147500"));
        when(providerPort.payPostpaid("530000000001", "pln", "TRX010")).thenReturn(payResp);
        when(transactionRepository.findById(TX_ID)).thenAnswer(inv -> {
            Transactions settled = new Transactions();
            settled.setId(TX_ID);
            settled.setRefNo("TRX010");
            settled.setStatus(TransactionStatus.SUCCESS);
            settled.setTotal(new BigDecimal("149000"));
            return Optional.of(settled);
        });

        TransactionDTO dto = service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001",
                null, new BigDecimal("149000"));

        ArgumentCaptor<Transactions> saved = ArgumentCaptor.forClass(Transactions.class);
        verify(transactionRepository).save(saved.capture());
        Transactions tx = saved.getValue();
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PROCESSING);
        assertThat(tx.getTargetNumber()).isEqualTo("530000000001");
        assertThat(tx.getPrice()).isEqualByComparingTo("145000");
        assertThat(tx.getAdminFee()).isEqualByComparingTo("4000"); // dfAdmin 2500 + markup 1500
        assertThat(tx.getTotal()).isEqualByComparingTo("149000");
        assertThat(tx.getRefNo()).isEqualTo("TRX010");
        assertThat(tx.getCustomerName()).isEqualTo("BUDI SANTOSO");

        verify(walletGateway).deductBalance(eq("wallet-1"), eq(new BigDecimal("149000")),
                eq(TX_ID), anyString());
        verify(providerPort).payPostpaid("530000000001", "pln", "TRX010");
        verify(transactionDomainService).reconcileProviderResult(same(tx), same(payResp),
                eq("wallet-1"), any(DenomInfo.class));
        assertThat(dto.status()).isEqualTo(TransactionStatus.SUCCESS);
    }

    @Test
    void billChangedSinceDisplayThrows409CodeBeforeAnyCharge() {
        stubFreshInquiry("150000"); // fresh total 154000 != expected 149000

        assertThatThrownBy(() -> service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001",
                null, new BigDecimal("149000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("BILL_CHANGED"));

        verifyNoInteractions(walletGateway);
        verify(transactionRepository, never()).save(any());
        verify(providerPort, never()).payPostpaid(any(), any(), any());
    }

    @Test
    void inquiryFailureAbortsWithoutChargeOrRow() {
        when(providerPort.inquiry("530000000001", "pln", "TRX010", null))
                .thenReturn(new InquiryResult(null, null, null, "14", "Nomor tidak ditemukan", null));

        assertThatThrownBy(() -> service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001",
                null, new BigDecimal("149000")))
                .isInstanceOf(SupplierException.class);

        verifyNoInteractions(walletGateway);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void doubleSubmitWithinOneMinuteIsBlocked() {
        when(transactionRepository.existsByStoreIdAndProductDenomIdAndTargetNumberAndStatusInAndCreatedAtAfter(
                eq(STORE_ID), eq(DENOM_ID), eq("530000000001"), anyList(), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001",
                null, new BigDecimal("149000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("DUPLICATE_TRANSACTION"));

        verifyNoInteractions(providerPort, walletGateway);
    }

    @Test
    void pendingPayDelegatesToReconcileAndNeverRefundsItself() throws Exception {
        stubFreshInquiry("145000");
        ProviderResponse pending = new ProviderResponse(ProviderStatus.PENDING, null, null,
                "timeout", null);
        when(providerPort.payPostpaid("530000000001", "pln", "TRX010")).thenReturn(pending);
        when(transactionRepository.findById(TX_ID)).thenReturn(Optional.empty());

        service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001", null, new BigDecimal("149000"));

        // refund on FAILED / hold on PENDING is reconcile's job (already covered by
        // TransactionDomainServiceTest) — the service must only delegate:
        verify(transactionDomainService).reconcileProviderResult(any(Transactions.class),
                same(pending), eq("wallet-1"), any(DenomInfo.class));
        verify(walletGateway, never()).refundBalance(any(), any(), any(), any());
    }

    @Test
    void failedPayDelegatesToReconcileAndNeverRefundsItself() throws Exception {
        stubFreshInquiry("145000");
        ProviderResponse failed = new ProviderResponse(ProviderStatus.FAILED, null, null,
                "Gagal di sisi supplier", null);
        when(providerPort.payPostpaid("530000000001", "pln", "TRX010")).thenReturn(failed);
        when(transactionRepository.findById(TX_ID)).thenReturn(Optional.empty());

        service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001", null, new BigDecimal("149000"));

        verify(transactionDomainService).reconcileProviderResult(any(Transactions.class),
                same(failed), eq("wallet-1"), any(DenomInfo.class));
        // the service itself never refunds directly — that's reconcileProviderResult's job
        verify(walletGateway, never()).refundBalance(any(), any(), any(), any());
    }

    @Test
    void amountRuleViolationIsRejectedBeforeInquiry() {
        // pascaDenom() is FIXED_DENOM — passing an amount must be rejected (reuse of Task 5 rule)
        assertThatThrownBy(() -> service.pay(STORE_ID, "wallet-1", DENOM_ID, "530000000001",
                new BigDecimal("50000"), new BigDecimal("149000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("AMOUNT_NOT_ALLOWED"));

        verifyNoInteractions(providerPort, walletGateway, transactionRepository);
    }
}
