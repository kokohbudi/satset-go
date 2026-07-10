package com.satset.supplier.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * {@link RestClient} untuk semua call supplier — koneksi langsung (home IP).
 *
 * <p>Satu interceptor nge-log setiap request/response supplier (price-list, saldo,
 * transaction, dst) — jadi tiap client baru gak perlu nulis log sendiri.
 * IP outbound di-log saat startup buat cek whitelist Digiflazz.
 */
@Configuration
public class ProviderHttpConfig {

    private static final Logger log = LoggerFactory.getLogger(ProviderHttpConfig.class);

    private static final ObjectMapper JSON = new ObjectMapper();

    // Redact field sensitif: kredensial request (sign/username/api_key) + response (sn/token = duit, customer_no = PII).
    private static final Pattern SENSITIVE = Pattern.compile(
            "\"(sign|username|api_key|sn|serial_number|customer_no|token|hp|msisdn)\"\\s*:\\s*\"[^\"]*\"");

    @Bean
    RestClient providerRestClient() {
        return RestClient.builder()
                // buffering: biar response body bisa dibaca interceptor DAN caller
                .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .requestInterceptor(ProviderHttpConfig::logSupplierCall)
                .build();
    }

    /**
     * Log semua call supplier di satu tempat: request body + response body utuh
     * (no truncate; kredensial sign/username/api_key + field sensitif di-redact
     * via {@link #SENSITIVE} sebelum masuk log).
     */
    private static ClientHttpResponse logSupplierCall(HttpRequest req, byte[] body,
            ClientHttpRequestExecution exec) throws IOException {
        String reqBody = new String(body, StandardCharsets.UTF_8);
        log.info("Supplier → {} {} request:\n{}", req.getMethod(), req.getURI(),
                redact(prettyJson(reqBody)));
        long t0 = System.nanoTime();
        ClientHttpResponse resp = exec.execute(req, body);
        String respBody = StreamUtils.copyToString(resp.getBody(), StandardCharsets.UTF_8);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        // full body, no truncate — logger com.satset.supplier punya file sendiri (logs/supplier/)
        log.info("Supplier ← {} {} → {} ({} ms, {} chars):\n{}", req.getMethod(), req.getURI(),
                resp.getStatusCode().value(), ms, respBody.length(), redact(prettyJson(respBody)));
        return resp;
    }

    /** Pretty-print JSON biar kebaca di log; kalau bukan JSON valid, apa adanya. */
    static String prettyJson(String raw) {
        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(JSON.readTree(raw));
        } catch (Exception e) {
            return raw;
        }
    }

    /** Redact nilai field sensitif dari JSON sebelum di-log. */
    static String redact(String json) {
        return SENSITIVE.matcher(json).replaceAll("\"$1\":\"***\"");
    }

    @EventListener(ApplicationReadyEvent.class)
    void logOutboundIp() {
        try {
            String ip = RestClient.create().get()
                    .uri("https://api.ipify.org")
                    .retrieve()
                    .body(String.class);
            log.warn("IP outbound saat ini: {} — pastikan terdaftar di whitelist IP Digiflazz", ip);
        } catch (Exception e) {
            // ponytail: gagal cek IP jangan gagalin startup, cuma warning
            log.warn("Gagal deteksi IP outbound: {}", e.getMessage());
        }
    }
}
