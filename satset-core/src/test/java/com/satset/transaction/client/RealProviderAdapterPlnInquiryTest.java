package com.satset.transaction.client;

import com.satset.digiflazz.client.DigiflazzClient;
import com.satset.transaction.model.PlnInquiryResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RealProviderAdapterPlnInquiryTest {

    private final DigiflazzClient df = mock(DigiflazzClient.class);
    private final RealProviderAdapter adapter = new RealProviderAdapter(df);

    @Test
    void plnInquiryMapsDigiResultToPlnInquiryResult() {
        when(df.inquiryPln("530000000001"))
                .thenReturn(new DigiflazzClient.DigiPlnInquiryResult("Sukses", "00", "530000000001",
                        "BUDI SANTOSO", "12345678901", "R1 /000001300", "Inquiry Sukses"));

        PlnInquiryResult r = adapter.plnInquiry("530000000001");

        assertThat(r.customerName()).isEqualTo("BUDI SANTOSO");
        assertThat(r.meterNo()).isEqualTo("12345678901");
        assertThat(r.segmentPower()).isEqualTo("R1 /000001300");
        assertThat(r.rc()).isEqualTo("00");
        assertThat(r.ok()).isTrue();
    }

    @Test
    void plnInquiryWithNonZeroRcIsNotOkAndEmptyNameBecomesNull() {
        when(df.inquiryPln("530000000001"))
                .thenReturn(new DigiflazzClient.DigiPlnInquiryResult("Gagal", "14", "530000000001",
                        "", "", "", "Nomor tidak ditemukan"));

        PlnInquiryResult r = adapter.plnInquiry("530000000001");

        assertThat(r.ok()).isFalse();
        assertThat(r.customerName()).isNull();
        assertThat(r.meterNo()).isNull();
        assertThat(r.segmentPower()).isNull();
        assertThat(r.message()).isEqualTo("Nomor tidak ditemukan");
    }
}
