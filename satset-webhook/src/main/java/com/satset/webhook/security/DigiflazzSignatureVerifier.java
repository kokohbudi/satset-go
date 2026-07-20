package com.satset.webhook.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Verifies Digiflazz's {@code X-Hub-Signature: sha1=<hex>} header — HMAC-SHA1
 * over the raw request body, per developer.digiflazz.com/api/buyer/webhook/.
 */
@Component
public class DigiflazzSignatureVerifier {

    private static final String ALGORITHM = "HmacSHA1";
    private static final String PREFIX = "sha1=";

    private final String secret;

    public DigiflazzSignatureVerifier(@Value("${digiflazz.webhook.secret:}") String secret) {
        this.secret = secret;
    }

    public boolean verify(String rawBody, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
            return false;
        }
        if (secret == null || secret.isBlank()) {
            // Not registered in DF's dashboard yet (WH-8) — reject cleanly, don't crash.
            return false;
        }
        String expectedHex = hmacSha1Hex(rawBody);
        String providedHex = signatureHeader.substring(PREFIX.length());
        return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.UTF_8),
                providedHex.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha1Hex(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA1 unavailable", e);
        }
    }
}
