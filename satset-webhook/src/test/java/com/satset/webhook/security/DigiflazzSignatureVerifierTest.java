package com.satset.webhook.security;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class DigiflazzSignatureVerifierTest {

    private static final String SECRET = "test-secret";
    private static final String BODY = "{\"data\":{\"ref_id\":\"30467470\",\"status\":\"Sukses\"}}";

    private final DigiflazzSignatureVerifier verifier = new DigiflazzSignatureVerifier(SECRET);

    @Test
    void acceptsValidSignature() {
        String header = "sha1=" + hmacSha1Hex(BODY, SECRET);

        assertThat(verifier.verify(BODY, header)).isTrue();
    }

    @Test
    void acceptsValidSignature_caseInsensitiveHex() {
        String header = "sha1=" + hmacSha1Hex(BODY, SECRET).toUpperCase();

        assertThat(verifier.verify(BODY, header)).isTrue();
    }

    @Test
    void rejectsTamperedBody() {
        String header = "sha1=" + hmacSha1Hex(BODY, SECRET);

        assertThat(verifier.verify(BODY + "x", header)).isFalse();
    }

    @Test
    void rejectsWrongSecret() {
        String header = "sha1=" + hmacSha1Hex(BODY, "wrong-secret");

        assertThat(verifier.verify(BODY, header)).isFalse();
    }

    @Test
    void rejectsMissingHeader() {
        assertThat(verifier.verify(BODY, null)).isFalse();
    }

    @Test
    void rejectsHeaderWithoutSha1Prefix() {
        String hex = hmacSha1Hex(BODY, SECRET);

        assertThat(verifier.verify(BODY, hex)).isFalse();
    }

    @Test
    void rejectsWithoutThrowing_whenSecretNotConfiguredYet() {
        // DIGIFLAZZ_WEBHOOK_SECRET defaults to "" until registered in DF's dashboard (WH-8) —
        // must reject cleanly (401), not crash (500), on real traffic in that window.
        DigiflazzSignatureVerifier unconfigured = new DigiflazzSignatureVerifier("");

        assertThat(unconfigured.verify(BODY, "sha1=" + hmacSha1Hex(BODY, SECRET))).isFalse();
    }

    private static String hmacSha1Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
