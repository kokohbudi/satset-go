package com.satset.digiflazz.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderHttpConfigTest {

    @Test
    void redact_masksSensitiveTransactionFields() {
        String tx = "{\"data\":{\"rc\":\"00\",\"sn\":\"SN123456\",\"customer_no\":\"08123\",\"buyer_sku_code\":\"s5\"}}";
        String out = ProviderHttpConfig.redact(tx);
        assertThat(out).contains("\"sn\":\"***\"");
        assertThat(out).contains("\"customer_no\":\"***\"");
        assertThat(out).doesNotContain("SN123456").doesNotContain("08123");
        assertThat(out).contains("\"buyer_sku_code\":\"s5\""); // non-sensitif tetap
    }

    @Test
    void redact_masksRequestCredentials() {
        String req = "{\"cmd\":\"prepaid\",\"username\":\"kokoh@mail.com\",\"sign\":\"abc123def\"}";
        String out = ProviderHttpConfig.redact(req);
        assertThat(out).contains("\"username\":\"***\"").contains("\"sign\":\"***\"");
        assertThat(out).doesNotContain("kokoh@mail.com").doesNotContain("abc123def");
        assertThat(out).contains("\"cmd\":\"prepaid\"");
    }

    @Test
    void redact_leavesPriceListUntouched() {
        String pl = "{\"data\":[{\"product_name\":\"Axis 10.000\",\"seller_name\":\"Ki***\",\"price\":10890}]}";
        assertThat(ProviderHttpConfig.redact(pl)).isEqualTo(pl);
    }
}
