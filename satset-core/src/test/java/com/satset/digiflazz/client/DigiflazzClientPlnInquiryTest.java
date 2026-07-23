package com.satset.digiflazz.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Prepaid PLN customer-name inquiry — POST /inquiry-pln. Different endpoint/shape from
 * inq-pasca (no commands, no ref_id, no buyer_sku_code, no testing flag — DF ignores testing
 * here and validates the signature against the production key regardless).
 */
class DigiflazzClientPlnInquiryTest {

    private MockRestServiceServer server;
    private DigiflazzClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DigiflazzClient(builder.build(),
                "https://api.digiflazz.com/v1/price-list",
                "https://api.digiflazz.com/v1/transaction",
                "https://api.digiflazz.com/v1/inquiry-pln", "u", "k", false);
    }

    @Test
    void inquiryPlnSendsCustomerNoAndCorrectSignAndParsesName() {
        server.expect(requestTo("https://api.digiflazz.com/v1/inquiry-pln"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.username").value("u"))
                .andExpect(jsonPath("$.customer_no").value("530000000001"))
                .andExpect(jsonPath("$.sign").value("97d579a9c43ffa53f4a3045feb443ce1"))
                .andExpect(jsonPath("$.commands").doesNotExist())
                .andExpect(jsonPath("$.ref_id").doesNotExist())
                .andExpect(jsonPath("$.buyer_sku_code").doesNotExist())
                .andExpect(jsonPath("$.testing").doesNotExist())
                .andRespond(withSuccess("""
                        {"data":{"customer_no":"530000000001","meter_no":"12345678901",
                         "subscriber_id":"530000000001","name":"BUDI SANTOSO",
                         "segment_power":"R1 /000001300","status":"Sukses","rc":"00",
                         "message":"Inquiry Sukses"}}
                        """, MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiPlnInquiryResult r = client.inquiryPln("530000000001");

        assertThat(r.status()).isEqualTo("Sukses");
        assertThat(r.rc()).isEqualTo("00");
        assertThat(r.customerNo()).isEqualTo("530000000001");
        assertThat(r.name()).isEqualTo("BUDI SANTOSO");
        assertThat(r.meterNo()).isEqualTo("12345678901");
        assertThat(r.segmentPower()).isEqualTo("R1 /000001300");
    }

    @Test
    void inquiryPlnReturnsHttpRcOnTransportError() {
        server.expect(requestTo("https://api.digiflazz.com/v1/inquiry-pln"))
                .andRespond(withServerError());

        DigiflazzClient.DigiPlnInquiryResult r = client.inquiryPln("530000000001");

        assertThat(r.status()).isNull();
        assertThat(r.rc()).isEqualTo("HTTP");
    }

    @Test
    void inquiryPlnReturnsParseRcOnGarbageBody() {
        server.expect(requestTo("https://api.digiflazz.com/v1/inquiry-pln"))
                .andRespond(withSuccess("not-json-at-all", MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiPlnInquiryResult r = client.inquiryPln("530000000001");

        assertThat(r.rc()).isEqualTo("PARSE");
    }
}
