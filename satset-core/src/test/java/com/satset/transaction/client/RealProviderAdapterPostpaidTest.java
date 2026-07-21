package com.satset.transaction.client;

import com.satset.digiflazz.client.DigiflazzClient;
import com.satset.transaction.model.InquiryResult;
import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.ProviderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealProviderAdapterPostpaidTest {

    private final DigiflazzClient df = mock(DigiflazzClient.class);
    private final RealProviderAdapter adapter = new RealProviderAdapter(df);

    @Test
    void inquiryMapsDigiResultToInquiryResult() {
        when(df.inquiry("ref1", "pln", "530000000001", null))
                .thenReturn(new DigiflazzClient.DigiInquiryResult("Sukses", "00", "ref1",
                        "BUDI SANTOSO", new BigDecimal("145000"), new BigDecimal("2500"),
                        null, "Inquiry Sukses", null));

        InquiryResult r = adapter.inquiry("530000000001", "pln", "ref1", null);

        assertThat(r.customerName()).isEqualTo("BUDI SANTOSO");
        assertThat(r.bill()).isEqualByComparingTo("145000");
        assertThat(r.admin()).isEqualByComparingTo("2500");
        assertThat(r.rc()).isEqualTo("00");
        assertThat(r.ok()).isTrue();
    }

    @Test
    void inquiryWithNonZeroRcIsNotOk() {
        when(df.inquiry("ref1", "pln", "530000000001", null))
                .thenReturn(new DigiflazzClient.DigiInquiryResult("Gagal", "14", "ref1",
                        null, null, null, null, "Nomor tidak ditemukan", null));

        InquiryResult r = adapter.inquiry("530000000001", "pln", "ref1", null);

        assertThat(r.ok()).isFalse();
        assertThat(r.message()).isEqualTo("Nomor tidak ditemukan");
    }

    @Test
    void payPostpaidMapsSuksesToSuccessWithStruk() {
        when(df.payPostpaid("ref1", "pln", "530000000001"))
                .thenReturn(new DigiflazzClient.DigiTxResult("Sukses", "00", "ref1",
                        "STRUK/PLN/1234567890", new BigDecimal("147500"), "Pembayaran Sukses"));

        ProviderResponse r = adapter.payPostpaid("530000000001", "pln", "ref1");

        assertThat(r.status()).isEqualTo(ProviderStatus.SUCCESS);
        assertThat(r.serialNumber()).isEqualTo("STRUK/PLN/1234567890");
        assertThat(r.cost()).isEqualByComparingTo("147500");
    }

    @Test
    void payPostpaidMapsGagalHardRcToFailed() {
        when(df.payPostpaid("ref1", "pln", "530000000001"))
                .thenReturn(new DigiflazzClient.DigiTxResult("Gagal", "99", "ref1",
                        null, null, "Gagal"));

        assertThat(adapter.payPostpaid("530000000001", "pln", "ref1").status())
                .isEqualTo(ProviderStatus.FAILED);
    }

    @Test
    void payPostpaidMapsHttpErrorToPending() {
        when(df.payPostpaid("ref1", "pln", "530000000001"))
                .thenReturn(new DigiflazzClient.DigiTxResult(null, "HTTP", "ref1",
                        null, null, "timeout"));

        assertThat(adapter.payPostpaid("530000000001", "pln", "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }
}
