package com.satset.webhook.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Own slim datasource/EMF config — deliberately NOT {@code CoreDataSourceConfig}
 * (which maps all 6 slices: catalog, identity, onboarding, transaction, quickmenu,
 * wallet). The webhook only ever touches catalog (DenomRepository), transaction,
 * and wallet — mapping the other 3 slices' entities just slows down cold-start
 * Hibernate metadata building for no benefit (this app auto-suspends between
 * Digiflazz callbacks, so every cold start pays this cost).
 *
 * <p>Component-scanned (not {@code @Import}ed on the main class) so
 * {@code @WebMvcTest}'s slice filter can exclude it — see prior javadoc history.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.satset.catalog.repository",
                "com.satset.transaction.repository",
                "com.satset.wallet.repository"
        },
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager")
public class WebhookDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        // @ConfigurationProperties above is REQUIRED — without it, a manually-built
        // DataSource ignores spring.datasource.hikari.* (pool size, minimum-idle,
        // max-lifetime, socketTimeout via data-source-properties). Boot only
        // auto-binds those to its own auto-configured DataSource, which this
        // @Primary bean replaces.
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder, DataSource dataSource,
            // "none" in prod: schema is already managed by satset-core (same Neon DB), so this
            // deploy never needs to validate/alter it — that round-trip cost was ~11s of cold
            // start (this app auto-suspends between callbacks, every hit pays that cost).
            // Tests override to "update" (fresh Testcontainers Postgres needs table creation).
            @Value("${webhook.hibernate.ddl-auto:none}") String ddlAuto) {

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", ddlAuto);
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.jdbc.batch_size", "25");

        return builder
                .dataSource(dataSource)
                .packages(
                        "com.satset.catalog.model",
                        "com.satset.transaction.model",
                        "com.satset.wallet.model")
                .persistenceUnit("core")
                .properties(jpaProperties)
                .build();
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @org.springframework.beans.factory.annotation.Qualifier("entityManagerFactory")
            LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}
