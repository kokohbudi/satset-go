package com.satset.transaction.service.prepaid;

import com.satset.shared.exception.SupplierException;
import com.satset.transaction.client.ProviderPort;
import com.satset.transaction.dto.PlnInquiryDTO;
import com.satset.transaction.model.PlnInquiryResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlnInquiryServiceTest {

    private final ProviderPort providerPort = mock(ProviderPort.class);
    private final PlnInquiryService service = new PlnInquiryService(providerPort);

    @Test
    void okResultReturnsPlnInquiryDTO() {
        when(providerPort.plnInquiry("530000000001"))
                .thenReturn(new PlnInquiryResult("BUDI SANTOSO", "12345678901", "R1 /000001300", "00", "Sukses"));

        PlnInquiryDTO dto = service.inquiry("530000000001");

        assertThat(dto.customerName()).isEqualTo("BUDI SANTOSO");
        assertThat(dto.meterNo()).isEqualTo("12345678901");
        assertThat(dto.segmentPower()).isEqualTo("R1 /000001300");
    }

    @Test
    void numericRcThrowsSupplierExceptionWithRawMessage() {
        when(providerPort.plnInquiry("530000000001"))
                .thenReturn(new PlnInquiryResult(null, null, null, "14", "Nomor tidak ditemukan"));

        assertThatThrownBy(() -> service.inquiry("530000000001"))
                .isInstanceOf(SupplierException.class)
                .hasMessage("Nomor tidak ditemukan");
    }

    @Test
    void httpRcYieldsGenericMessageNotRawDetail() {
        when(providerPort.plnInquiry("530000000001"))
                .thenReturn(new PlnInquiryResult(null, null, null, "HTTP",
                        "I/O error on POST request for \"https://api.digiflazz.com/v1\": Connection timed out"));

        assertThatThrownBy(() -> service.inquiry("530000000001"))
                .isInstanceOf(SupplierException.class)
                .hasMessageNotContaining("Connection timed out")
                .hasMessageNotContaining("digiflazz")
                .hasMessage("Layanan supplier sedang tidak tersedia. Coba lagi sebentar.");
    }

    @Test
    void parseRcYieldsGenericMessageNotRawDetail() {
        when(providerPort.plnInquiry("530000000001"))
                .thenReturn(new PlnInquiryResult(null, null, null, "PARSE", "Unexpected token"));

        assertThatThrownBy(() -> service.inquiry("530000000001"))
                .isInstanceOf(SupplierException.class)
                .hasMessageNotContaining("Unexpected token")
                .hasMessage("Layanan supplier sedang tidak tersedia. Coba lagi sebentar.");
    }
}
