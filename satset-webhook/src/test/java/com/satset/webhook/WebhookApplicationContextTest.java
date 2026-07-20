package com.satset.webhook;

import com.satset.transaction.service.topup.TransactionDomainService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the module scaffold actually wires: the scan set in
 * {@link WebhookApplication} plus the imported {@code CoreDataSourceConfig}
 * must produce a working {@link TransactionDomainService} bean against a real
 * (Testcontainers) Postgres — the only way to prove a component-scan list is
 * correct.
 */
@SpringBootTest(classes = WebhookApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WebhookApplicationContextTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Fresh Testcontainers Postgres has no tables yet — prod defaults to "none"
        // (schema already managed by satset-core against the shared Neon DB).
        registry.add("webhook.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private TransactionDomainService transactionDomainService;

    @Test
    void contextLoads() {
        assertThat(transactionDomainService).isNotNull();
    }
}
