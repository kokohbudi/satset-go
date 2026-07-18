package com.satset.supplier.client;

import com.satset.transaction.model.ProviderResponse;
import com.satset.transaction.model.ProviderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RealProviderAdapterTest {

    private final DigiflazzClient df = mock(DigiflazzClient.class);
    private final RealProviderAdapter adapter = new RealProviderAdapter(df);

    private void stub(String status, String rc) {
        when(df.topup("ref1", "xld25", "0878")).thenReturn(
                new DigiflazzClient.DigiTxResult(status, rc, "ref1", "SN9", new BigDecimal("24500"), "msg"));
    }

    @Test void sukses_mapsSuccess() {
        stub("Sukses", "00");
        ProviderResponse r = adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1");
        assertThat(r.status()).isEqualTo(ProviderStatus.SUCCESS);
        assertThat(r.serialNumber()).isEqualTo("SN9");
        assertThat(r.cost()).isEqualByComparingTo("24500");
    }

    @Test void pending_mapsPending() {
        stub("Pending", "03");
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }

    @Test void gagalTimeout_mapsPending_moneySafe() {
        stub("Gagal", "01");   // timeout may have formed a tx -> do NOT refund
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }

    @Test void gagalNotFound_mapsPending_moneySafe() {
        stub("Gagal", "50");
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }

    @Test void gagalTerminal_mapsFailed() {
        stub("Gagal", "44");   // insufficient DF balance -> real failure -> refund
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.FAILED);
    }

    @Test void unknownStatus_mapsPending_moneySafe() {
        stub(null, "PARSE");
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }

    @Test void gagalNullRc_mapsPending_noNpe() {
        when(df.topup("ref1", "xld25", "0878")).thenReturn(
                new DigiflazzClient.DigiTxResult("Gagal", null, "ref1", "SN9", new BigDecimal("24500"), "msg"));
        assertThat(adapter.sendTransaction("0878", "xld25", new BigDecimal("25000"), "ref1").status())
                .isEqualTo(ProviderStatus.PENDING);
    }
}
