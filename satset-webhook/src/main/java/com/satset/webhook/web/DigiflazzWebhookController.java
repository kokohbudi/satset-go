package com.satset.webhook.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.satset.shared.exception.ResourceNotFoundException;
import com.satset.webhook.dto.DigiflazzWebhookPayload;
import com.satset.webhook.security.DigiflazzSignatureVerifier;
import com.satset.webhook.service.DigiflazzWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Digiflazz's only entry point into this deploy. Binds the body as a raw
 * {@code String} (not a parsed DTO) so signature verification runs over the
 * exact bytes Digiflazz signed, per docs/superpowers/specs/2026-07-20-webhook-split-deploy-design.md.
 */
@Slf4j
@RestController
public class DigiflazzWebhookController {

    // Not Spring-managed: Boot's autoconfigured ObjectMapper bean is Jackson 3
    // (tools.jackson) by default, but this DTO uses classic Jackson 2
    // annotations (matches DigiflazzClient's own MAPPER field).
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DigiflazzSignatureVerifier verifier;
    private final DigiflazzWebhookService service;

    public DigiflazzWebhookController(DigiflazzSignatureVerifier verifier,
                                       DigiflazzWebhookService service) {
        this.verifier = verifier;
        this.service = service;
    }

    @PostMapping("/api/webhooks/digiflazz")
    public ResponseEntity<Void> handle(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature", required = false) String signature) {

        if (!verifier.verify(rawBody, signature)) {
            log.warn("Webhook signature rejected");
            return ResponseEntity.status(401).build();
        }

        DigiflazzWebhookPayload payload;
        try {
            payload = MAPPER.readValue(rawBody, DigiflazzWebhookPayload.class);
        } catch (JsonProcessingException e) {
            log.warn("Webhook payload malformed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        log.info("Webhook received: refId={} status={} rc={}",
                payload.data().refId(), payload.data().status(), payload.data().rc());

        try {
            service.handle(payload.data());
        } catch (ResourceNotFoundException e) {
            log.warn("Webhook refId={} not found", payload.data().refId());
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            log.error("Webhook settle failed: refId={}", payload.data().refId(), e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }
}
