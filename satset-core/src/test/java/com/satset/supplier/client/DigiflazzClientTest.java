package com.satset.supplier.client;

import com.satset.shared.exception.SupplierException;
import com.satset.supplier.model.PriceListItem;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit test {@link DigiflazzClient#fetchSnapshot()}: signing benar (md5 user+key+"pricelist"),
 * POST ke /price-list dgn cmd=prepaid, dan parse {@code {"data":[...]}} ke {@link PriceListItem}.
 * Pakai {@link MockRestServiceServer} — no network.
 */
class DigiflazzClientTest {

    @Test
    void fetchSnapshot_postsSignedRequest_andParsesData() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // sign = md5("user1" + "key1" + "pricelist")
        server.expect(requestTo("https://api.digiflazz.com/v1/price-list"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.cmd").value("prepaid"))
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.sign").value("34e5497a653a80fe9e458458536ac7ce"))
                .andRespond(withSuccess("""
                        {"data":[
                          {"product_name":"Telkomsel 5000","category":"Pulsa","brand":"TELKOMSEL",
                           "type":"Umum","buyer_sku_code":"tsel5","price":5450,
                           "buyer_product_status":true,"seller_product_status":true,
                           "unlimited_stock":false,"stock":"100","seller_name":"Ki***",
                           "multi":true,"start_cut_off":"23:30","end_cut_off":"0:30","desc":"ok"}
                        ]}""", APPLICATION_JSON));

        DigiflazzClient client = new DigiflazzClient(
                builder.build(), "https://api.digiflazz.com/v1", "user1", "key1");

        List<PriceListItem> items = client.fetchSnapshot().items();

        assertThat(items).hasSize(1);
        PriceListItem it = items.get(0);
        assertThat(it.productName()).isEqualTo("Telkomsel 5000");
        assertThat(it.brand()).isEqualTo("TELKOMSEL");
        assertThat(it.buyerSkuCode()).isEqualTo("tsel5");
        assertThat(it.price()).isEqualTo(5450L);
        assertThat(it.buyerProductStatus()).isTrue();
        assertThat(it.stock()).isEqualTo("100");
        assertThat(it.sellerName()).isEqualTo("Ki***");
        server.verify();
    }

    @Test
    void fetchSnapshot_errorResponse_throwsCleanException_notJacksonCrash() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        // DF rate-limit (rc 83): data = object {rc,message}, bukan array
        server.expect(requestTo("https://api.digiflazz.com/v1/price-list"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"data":{"rc":"83","message":"Anda telah mencapai limitasi pengecekan pricelist, silahkan coba beberapa saat lagi"}}""",
                        APPLICATION_JSON));

        DigiflazzClient client = new DigiflazzClient(
                builder.build(), "https://api.digiflazz.com/v1", "user1", "key1");

        assertThatThrownBy(() -> client.fetchSnapshot().items())
                .isInstanceOf(SupplierException.class)
                .hasMessageContaining("limitasi pengecekan pricelist")   // pesan asli DF diteruskan
                .extracting(e -> ((SupplierException) e).getCode())
                .isEqualTo("83");                                        // rc DF diteruskan apa adanya
    }
}
