package com.satset.transaction.service.postpaid;

import com.satset.catalog.model.DenomType;
import com.satset.catalog.repository.DenomRepository;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.shared.exception.SupplierException;
import com.satset.shared.model.DenomInfo;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.client.WalletGateway;
import com.satset.transaction.dto.InquiryDTO;
import com.satset.transaction.model.InquiryResult;
import com.satset.transaction.repository.TransactionRepository;
import com.satset.transaction.service.topup.RefNoGenerator;
import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostpaidServiceInquiryTest {

    private static final UUID DENOM_ID = UUID.randomUUID();

    private final DenomRepository denomRepository = mock(DenomRepository.class);
    private final WalletGateway walletGateway = mock(WalletGateway.class);
    private final ProviderPort providerPort = mock(ProviderPort.class);
    private final RefNoGenerator refNoGenerator = mock(RefNoGenerator.class);
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final TransactionDomainService transactionDomainService = mock(TransactionDomainService.class);

    private final PostpaidService service = new PostpaidService(denomRepository, walletGateway,
            providerPort, refNoGenerator, transactionRepository, transactionDomainService);

    private static DenomInfo pascaDenom() { // FIXED_DENOM + requiresInquiry, markup 1500
        return new DenomInfo(DENOM_ID, "pln", "PLN Pascabayar", "PLN", BigDecimal.ZERO,
                new BigDecimal("1500"), BigDecimal.ZERO, true, false,
                true, DenomType.FIXED_DENOM, null, null);
    }

    private static DenomInfo emoneyDenom() { // OPEN_AMOUNT + requiresInquiry, 10k..1jt, markup 1000
        return new DenomInfo(DENOM_ID, "gopay", "GoPay Saldo", "GoPay", BigDecimal.ZERO,
                new BigDecimal("1000"), BigDecimal.ZERO, true, false,
                true, DenomType.OPEN_AMOUNT, new BigDecimal("10000"), new BigDecimal("1000000"));
    }

    @Test
    void fixedDenomInquiryReturnsBillWithMarkup() throws Exception {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(pascaDenom()));
        when(refNoGenerator.next()).thenReturn("TRX001");
        when(providerPort.inquiry("530000000001", "pln", "TRX001", null))
                .thenReturn(new InquiryResult("BUDI SANTOSO", new BigDecimal("145000"),
                        new BigDecimal("2500"), "00", "Sukses", null));

        InquiryDTO dto = service.inquiry(DENOM_ID, "530000000001", null);

        assertThat(dto.customerName()).isEqualTo("BUDI SANTOSO");
        assertThat(dto.bill()).isEqualByComparingTo("145000");
        assertThat(dto.admin()).isEqualByComparingTo("2500");
        assertThat(dto.markup()).isEqualByComparingTo("1500");
        assertThat(dto.total()).isEqualByComparingTo("149000");
        verifyNoInteractions(walletGateway, transactionRepository);
    }

    @Test
    void openAmountInquiryPassesAmountToProvider() throws Exception {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(emoneyDenom()));
        when(refNoGenerator.next()).thenReturn("TRX002");
        when(providerPort.inquiry("0812345678", "gopay", "TRX002", new BigDecimal("25000")))
                .thenReturn(new InquiryResult("BUDI", new BigDecimal("25000"),
                        new BigDecimal("1000"), "00", "Sukses", null));

        InquiryDTO dto = service.inquiry(DENOM_ID, "0812345678", new BigDecimal("25000"));

        assertThat(dto.total()).isEqualByComparingTo("27000"); // 25000 + 1000 + 1000
        verify(providerPort).inquiry("0812345678", "gopay", "TRX002", new BigDecimal("25000"));
    }

    @Test
    void openAmountExactBoundariesAreAccepted() throws Exception {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(emoneyDenom()));
        when(refNoGenerator.next()).thenReturn("TRXMIN", "TRXMAX");
        // amount == minAmount (10000) and == maxAmount (1000000) must both be ALLOWED (inclusive bounds)
        when(providerPort.inquiry("0812345678", "gopay", "TRXMIN", new BigDecimal("10000")))
                .thenReturn(new InquiryResult("BUDI", new BigDecimal("10000"),
                        new BigDecimal("1000"), "00", "Sukses", null));
        when(providerPort.inquiry("0812345678", "gopay", "TRXMAX", new BigDecimal("1000000")))
                .thenReturn(new InquiryResult("BUDI", new BigDecimal("1000000"),
                        new BigDecimal("1000"), "00", "Sukses", null));

        assertThat(service.inquiry(DENOM_ID, "0812345678", new BigDecimal("10000")).total())
                .isEqualByComparingTo("12000"); // 10000 + 1000 + 1000
        assertThat(service.inquiry(DENOM_ID, "0812345678", new BigDecimal("1000000")).total())
                .isEqualByComparingTo("1002000"); // 1000000 + 1000 + 1000
    }

    @Test
    void unknownDenomThrowsResourceNotFound() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void nonPostpaidDenomIsRejected() {
        DenomInfo prepaid = new DenomInfo(DENOM_ID, "tsel10", "Telkomsel 10k", "Telkomsel",
                new BigDecimal("11000"), new BigDecimal("500"), new BigDecimal("10500"), true, false,
                false, DenomType.FIXED_DENOM, null, null);
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(prepaid));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("NOT_POSTPAID"));
    }

    @Test
    void unavailableDenomIsRejected() {
        DenomInfo inactive = new DenomInfo(DENOM_ID, "pln", "PLN Pascabayar", "PLN",
                BigDecimal.ZERO, new BigDecimal("1500"), BigDecimal.ZERO, false, false,
                true, DenomType.FIXED_DENOM, null, null);
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("DENOM_UNAVAILABLE"));
    }

    @Test
    void fixedDenomWithAmountIsRejected() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(pascaDenom()));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", new BigDecimal("50000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("AMOUNT_NOT_ALLOWED"));
        verifyNoInteractions(providerPort);
    }

    @Test
    void openAmountWithoutAmountIsRejected() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(emoneyDenom()));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "0812345678", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("AMOUNT_REQUIRED"));
    }

    @Test
    void openAmountOutOfRangeIsRejected() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(emoneyDenom()));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "0812345678", new BigDecimal("5000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("AMOUNT_OUT_OF_RANGE"));
        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "0812345678", new BigDecimal("2000000")))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("AMOUNT_OUT_OF_RANGE"));
        verifyNoInteractions(providerPort);
    }

    @Test
    void transportErrorYieldsGenericMessageNotRawDetail() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(pascaDenom()));
        when(refNoGenerator.next()).thenReturn("TRX004");
        // rc "HTTP" carries raw Java exception text — must NOT reach the client (project rule).
        when(providerPort.inquiry(any(), any(), any(), any()))
                .thenReturn(new InquiryResult(null, null, null, "HTTP",
                        "I/O error on POST request for \"https://api.digiflazz.com/v1\": Connection timed out", null));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", null))
                .isInstanceOf(SupplierException.class)
                .hasMessageNotContaining("Connection timed out")
                .hasMessageNotContaining("digiflazz")
                .hasMessage("Layanan supplier sedang tidak tersedia. Coba lagi sebentar.");
    }

    @Test
    void supplierFailureThrowsSupplierException() {
        when(denomRepository.findDenomInfoById(DENOM_ID)).thenReturn(Optional.of(pascaDenom()));
        when(refNoGenerator.next()).thenReturn("TRX003");
        when(providerPort.inquiry(any(), any(), any(), any()))
                .thenReturn(new InquiryResult(null, null, null, "14", "Nomor tidak ditemukan", null));

        assertThatThrownBy(() -> service.inquiry(DENOM_ID, "530000000001", null))
                .isInstanceOf(SupplierException.class)
                .hasMessage("Nomor tidak ditemukan");
    }
}
