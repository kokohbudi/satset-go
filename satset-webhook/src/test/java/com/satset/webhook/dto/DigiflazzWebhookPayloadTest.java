package com.satset.webhook.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.satset.transaction.model.ProviderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DigiflazzWebhookPayloadTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesSampleSuksesPayload() throws Exception {
        String json = """
                { "data": { "ref_id": "30467470", "customer_no": "081280556115",
                  "buyer_sku_code": "ovo100", "message": "Sukses", "status": "Sukses",
                  "rc": "00", "buyer_last_saldo": 326719460,
                  "sn": "SEPTIAPAR/20190401214753214742", "price": 199800,
                  "tele": "@telegram", "wa": "081234512345" } }
                """;

        DigiflazzWebhookPayload payload = mapper.readValue(json, DigiflazzWebhookPayload.class);

        assertThat(payload.data().refId()).isEqualTo("30467470");
        assertThat(payload.data().status()).isEqualTo("Sukses");
        assertThat(payload.data().rc()).isEqualTo("00");
        assertThat(payload.data().sn()).isEqualTo("SEPTIAPAR/20190401214753214742");
        assertThat(payload.data().price()).isEqualByComparingTo(new BigDecimal("199800"));
    }

    @Test
    void toProviderResponse_mapsSukses_toSuccess() throws Exception {
        DigiflazzWebhookPayload payload = parse("Sukses", "00", "SN-1", "199800");

        var response = payload.data().toProviderResponse();

        assertThat(response.status()).isEqualTo(ProviderStatus.SUCCESS);
        assertThat(response.referenceNumber()).isEqualTo("30467470");
        assertThat(response.serialNumber()).isEqualTo("SN-1");
        assertThat(response.cost()).isEqualByComparingTo(new BigDecimal("199800"));
    }

    @Test
    void toProviderResponse_blankSn_mapsToNull() throws Exception {
        DigiflazzWebhookPayload payload = parse("Sukses", "00", "", "199800");

        assertThat(payload.data().toProviderResponse().serialNumber()).isNull();
    }

    @Test
    void toProviderResponse_gagalRc01_mapsToPending() throws Exception {
        DigiflazzWebhookPayload payload = parse("Gagal", "01", null, "0");

        assertThat(payload.data().toProviderResponse().status()).isEqualTo(ProviderStatus.PENDING);
    }

    private DigiflazzWebhookPayload parse(String status, String rc, String sn, String price) throws Exception {
        String snField = sn == null ? "null" : "\"" + sn + "\"";
        String json = """
                { "data": { "ref_id": "30467470", "customer_no": "081280556115",
                  "buyer_sku_code": "ovo100", "message": "%s", "status": "%s",
                  "rc": "%s", "buyer_last_saldo": 0,
                  "sn": %s, "price": %s } }
                """.formatted(status, status, rc, snField, price);
        return mapper.readValue(json, DigiflazzWebhookPayload.class);
    }
}
