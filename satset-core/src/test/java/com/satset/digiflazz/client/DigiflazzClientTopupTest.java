package com.satset.digiflazz.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DigiflazzClientTopupTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private DigiflazzClient client;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient rc = builder.build();
        // username="u", apiKey="k" -> sign = md5("u" + "k" + "ref1"); testing=false (prod default)
        client = new DigiflazzClient(rc, "https://api.digiflazz.com/v1", "u", "k", false);
    }

    @Test
    void topup_sendsSignedRequest_parsesPending() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
              .andExpect(method(org.springframework.http.HttpMethod.POST))
              .andExpect(jsonPath("$.buyer_sku_code").value("xld25"))
              .andExpect(jsonPath("$.customer_no").value("0878"))
              .andExpect(jsonPath("$.ref_id").value("ref1"))
              // md5("ukref1")
              .andExpect(jsonPath("$.sign").value("c28850e81191973e911ac305b9cc7c42"))
              .andExpect(jsonPath("$.testing").value(false))   // prod default: no test mode
              .andRespond(withSuccess("""
                  {"data":{"ref_id":"ref1","customer_no":"0878","buyer_sku_code":"xld25",
                  "message":"Transaksi Pending","status":"Pending","rc":"03","sn":"",
                  "buyer_last_saldo":100000,"price":25000}}
                  """, MediaType.APPLICATION_JSON));

        var r = client.topup("ref1", "xld25", "0878");

        assertThat(r.status()).isEqualTo("Pending");
        assertThat(r.rc()).isEqualTo("03");
        assertThat(r.price()).isEqualByComparingTo(new BigDecimal("25000"));
        server.verify();
    }

    @Test
    void topup_parsesSukses() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
              .andRespond(withSuccess("""
                  {"data":{"ref_id":"ref1","status":"Sukses","rc":"00",
                  "sn":"SN123","price":24500,"message":"Sukses"}}
                  """, MediaType.APPLICATION_JSON));

        var r = client.topup("ref1", "xld25", "0878");

        assertThat(r.status()).isEqualTo("Sukses");
        assertThat(r.sn()).isEqualTo("SN123");
        assertThat(r.price()).isEqualByComparingTo(new BigDecimal("24500"));
    }

    @Test
    void topup_testingMode_sendsTestingTrue() {
        DigiflazzClient testClient = new DigiflazzClient(builder.build(),
                "https://api.digiflazz.com/v1", "u", "k", true);
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
              .andExpect(jsonPath("$.testing").value(true))   // dev test mode -> DF canned response, no charge
              .andRespond(withSuccess("""
                  {"data":{"ref_id":"ref1","status":"Sukses","rc":"00","sn":"SN1","price":10000}}
                  """, MediaType.APPLICATION_JSON));

        var r = testClient.topup("ref1", "xld10", "087800001230");

        assertThat(r.status()).isEqualTo("Sukses");
        server.verify();
    }

    @Test
    void topup_malformedBody_returnsUnknownStatus() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
              .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        var r = client.topup("ref1", "xld25", "0878");

        assertThat(r.status()).isNull();   // adapter maps null-status -> PENDING (money-safe)
        assertThat(r.rc()).isEqualTo("PARSE");
    }

    @Test
    void topup_httpError_returnsPendingSafeResult_noThrow() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
              .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());
        var r = client.topup("ref1", "xld25", "0878");
        assertThat(r.status()).isNull();
        assertThat(r.rc()).isEqualTo("HTTP");
    }
}
