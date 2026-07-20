package com.satset.webhook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Standalone Digiflazz webhook receiver — deployed to Fly.io while
 * {@code satset-core} stays local-dev-only. Reuses core's domain/service code
 * directly (compile dependency, same Neon DB) instead of duplicating logic;
 * folds back into satset-core once it deploys to prod
 * (docs/superpowers/specs/2026-07-20-webhook-split-deploy-design.md).
 *
 * <p>Scan is limited to what {@code TransactionDomainService.reconcileProviderResult}
 * actually needs (transaction service/client, wallet service, Digiflazz client,
 * LogContext infra + {@link com.satset.webhook.config.WebhookDataSourceConfig}) —
 * deliberately excludes {@code com.satset.shared.config} (core's own Keycloak
 * SecurityConfig, DataSeeder, WebMvcConfig), {@code com.satset.transaction.web}
 * and {@code com.satset.wallet.web} (admin UI controllers needing beans this
 * deploy doesn't have, e.g. {@code StoreRepository}) so none of that loads here.
 */
@SpringBootApplication(scanBasePackages = {
        "com.satset.webhook",
        "com.satset.transaction.service.topup",
        "com.satset.transaction.client",
        "com.satset.wallet.service",
        "com.satset.digiflazz.client",
        "com.satset.shared.logging"
})
public class WebhookApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookApplication.class, args);
    }
}
