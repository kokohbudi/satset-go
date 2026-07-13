package com.satset.supplier.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.satset.shared.exception.SupplierException;
import com.satset.supplier.model.PriceListItem;
import com.satset.supplier.model.PriceListSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Client Digiflazz — nembak lewat {@code providerRestClient} (proxy Fly, egress statis
 * di-whitelist). Awalnya cuma daftar-harga ({@code /v1/price-list}) buat admin.
 *
 * <p>Sign Digiflazz = md5(username + apiKey + kata-kunci-cmd). Untuk price-list
 * kata-kuncinya {@code "pricelist"} (lihat {@code fly-proxy/df.sh}).
 */
@Slf4j
@Service
public class DigiflazzClient {

    // DF kirim field ekstra (multi, start_cut_off, end_cut_off) yg tak dipetakan — abaikan.
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<PriceListItem>> LIST_TYPE = new TypeReference<>() {};

    private final RestClient http;
    private final String baseUrl;
    private final String username;
    private final String apiKey;

    public DigiflazzClient(
            RestClient providerRestClient,
            @Value("${supplier.digiflazz.base-url:https://api.digiflazz.com/v1}") String baseUrl,
            @Value("${supplier.digiflazz.username:}") String username,
            @Value("${supplier.digiflazz.api-key:}") String apiKey) {
        this.http = providerRestClient;
        this.baseUrl = baseUrl;
        this.username = username;
        this.apiKey = apiKey;
    }

    /**
     * Cached snapshot (items + fill time). Di-cache 5 jam ({@code digiflazzCacheManager}) karena DF
     * nge-rate-limit endpoint ini (rc 83) dan datanya lag 10-15 menit — jadi cukup 1 hit / 5 jam.
     * {@code fetchedAt} = momen cache-miss (baru di-fill); cache-hit tetap pakai timestamp lama.
     */
    @Cacheable(value = "digiflazzPriceList", cacheManager = "digiflazzCacheManager", sync = true)
    public PriceListSnapshot fetchSnapshot() {
        return new PriceListSnapshot(doFetchPriceList(), LocalDateTime.now());
    }

    /**
     * Ambil daftar harga prepaid dari Digiflazz (HTTP call langsung, tanpa cache).
     *
     * <p>Response sukses: {@code {"data":[...]}}. Response error (mis. limit): {@code {"data":{"rc","message"}}}
     * — dideteksi (data bukan array) dan dilempar {@link IllegalStateException}, bukan crash parser.
     */
    private List<PriceListItem> doFetchPriceList() {
        var req = new PriceListRequest("prepaid", username, sign("pricelist"));
        String raw = http.post()
                .uri(baseUrl + "/price-list")
                .contentType(APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(String.class);
        if (raw == null || raw.isBlank()) return List.of();
        try {
            JsonNode data = MAPPER.readTree(raw).path("data");
            if (data.isArray()) {
                return MAPPER.convertValue(data, LIST_TYPE);
            }
            // data = object -> DF error (rc 83 = limit price-list, dll). Teruskan rc + pesan asli DF ke admin.
            String rc = data.path("rc").asText("");
            String msg = data.path("message").asText("Supplier tidak tersedia");
            log.error("Digiflazz price-list ditolak rc={} msg={}", rc, msg);
            throw new SupplierException(rc, msg);
        } catch (JsonProcessingException e) {
            log.error("Gagal parse respons Digiflazz price-list", e);
            throw new SupplierException("PARSE", "Respons Digiflazz tidak valid");
        }
    }

    private String sign(String cmdKeyword) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest((username + apiKey + cmdKeyword).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 tidak tersedia", e); // JDK selalu punya MD5
        }
    }

    private record PriceListRequest(String cmd, String username, String sign) {}
}
