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

class DigiflazzClientPascaTest {

    private MockRestServiceServer server;
    private DigiflazzClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DigiflazzClient(builder.build(), "https://api.digiflazz.com/v1", "u", "k", false);
    }

    @Test
    void inquirySendsInqPascaWithoutAmountAndParsesBill() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.commands").value("inq-pasca"))
                .andExpect(jsonPath("$.username").value("u"))
                .andExpect(jsonPath("$.buyer_sku_code").value("pln"))
                .andExpect(jsonPath("$.customer_no").value("530000000001"))
                .andExpect(jsonPath("$.ref_id").value("ref1"))
                .andExpect(jsonPath("$.sign").value("c28850e81191973e911ac305b9cc7c42"))
                .andExpect(jsonPath("$.amount").doesNotExist())
                .andRespond(withSuccess("""
                        {"data":{"ref_id":"ref1","customer_no":"530000000001",
                         "customer_name":"BUDI SANTOSO","buyer_sku_code":"pln",
                         "admin":2500,"price":145000,"selling_price":147500,
                         "rc":"00","status":"Sukses","message":"Inquiry Sukses",
                         "desc":{"tarif":"R1","daya":1300,"lembar_tagihan":1}}}
                        """, MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiInquiryResult r = client.inquiry("ref1", "pln", "530000000001", null);

        assertThat(r.status()).isEqualTo("Sukses");
        assertThat(r.rc()).isEqualTo("00");
        assertThat(r.refId()).isEqualTo("ref1");
        assertThat(r.customerName()).isEqualTo("BUDI SANTOSO");
        assertThat(r.price()).isEqualByComparingTo("145000");
        assertThat(r.admin()).isEqualByComparingTo("2500");
        assertThat(r.desc().path("tarif").asText()).isEqualTo("R1");
    }

    @Test
    void inquirySendsAmountWhenProvided() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andExpect(jsonPath("$.commands").value("inq-pasca"))
                .andExpect(jsonPath("$.amount").value(25000))
                .andRespond(withSuccess("""
                        {"data":{"ref_id":"ref1","customer_no":"0812345678","customer_name":"BUDI",
                         "buyer_sku_code":"gopay","admin":1000,"price":25000,"selling_price":26000,
                         "rc":"00","status":"Sukses","message":"Inquiry Sukses","desc":{}}}
                        """, MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiInquiryResult r =
                client.inquiry("ref1", "gopay", "0812345678", new java.math.BigDecimal("25000"));

        assertThat(r.rc()).isEqualTo("00");
        assertThat(r.price()).isEqualByComparingTo("25000");
    }

    @Test
    void inquiryReturnsHttpRcOnTransportError() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andRespond(withServerError());

        DigiflazzClient.DigiInquiryResult r = client.inquiry("ref1", "pln", "530000000001", null);

        assertThat(r.status()).isNull();
        assertThat(r.rc()).isEqualTo("HTTP");
    }

    @Test
    void inquiryReturnsParseRcOnGarbageBody() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andRespond(withSuccess("not-json-at-all", MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiInquiryResult r = client.inquiry("ref1", "pln", "530000000001", null);

        assertThat(r.rc()).isEqualTo("PARSE");
    }

    @Test
    void inquiryReturnsSafeNonSuccessResultOnEmptyBody() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiInquiryResult r = client.inquiry("ref1", "pln", "530000000001", null);

        // Empty 2xx body: readTree("") -> MissingNode, no throw. Money-safe: status null,
        // rc not "00" (non-success), so the caller never treats it as a paid inquiry.
        assertThat(r.status()).isNull();
        assertThat(r.rc()).isNotEqualTo("00");
    }

    @Test
    void payPostpaidSendsPayPascaWithoutAmountAndParsesStruk() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.commands").value("pay-pasca"))
                .andExpect(jsonPath("$.ref_id").value("ref1"))
                .andExpect(jsonPath("$.sign").value("c28850e81191973e911ac305b9cc7c42"))
                .andExpect(jsonPath("$.amount").doesNotExist())
                .andRespond(withSuccess("""
                        {"data":{"ref_id":"ref1","customer_no":"530000000001","buyer_sku_code":"pln",
                         "admin":2500,"price":147500,"selling_price":149000,"rc":"00","status":"Sukses",
                         "sn":"STRUK/PLN/1234567890","message":"Pembayaran Sukses"}}
                        """, MediaType.APPLICATION_JSON));

        DigiflazzClient.DigiTxResult r = client.payPostpaid("ref1", "pln", "530000000001");

        assertThat(r.status()).isEqualTo("Sukses");
        assertThat(r.rc()).isEqualTo("00");
        assertThat(r.sn()).isEqualTo("STRUK/PLN/1234567890");
        assertThat(r.price()).isEqualByComparingTo("147500");
    }

    @Test
    void payPostpaidReturnsHttpRcOnTransportError() {
        server.expect(requestTo("https://api.digiflazz.com/v1/transaction"))
                .andRespond(withServerError());

        DigiflazzClient.DigiTxResult r = client.payPostpaid("ref1", "pln", "530000000001");

        assertThat(r.status()).isNull();
        assertThat(r.rc()).isEqualTo("HTTP");
    }
}
