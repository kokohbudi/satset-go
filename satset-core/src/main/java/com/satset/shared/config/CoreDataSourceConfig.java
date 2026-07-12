package com.satset.shared.config;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
 * Single application datasource — backs every slice, including wallet (kept in
 * its own {@code satset_wallet} schema on the same database). Explicit config
 * remains because this bean set is {@code @Primary}-qualified; one datasource
 * means wallet + core writes now share one transaction (atomic purchases).
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.satset.catalog.repository",
                "com.satset.identity.repository",
                "com.satset.onboarding.repository",
                "com.satset.transaction.repository",
                "com.satset.quickmenu.repository",
                "com.satset.wallet.repository"
        },
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager")
public class CoreDataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    public DataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder, DataSource dataSource) {

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.jdbc.batch_size", "25");

        return builder
                .dataSource(dataSource)
                .packages(
                        "com.satset.catalog.model",
                        "com.satset.identity.model",
                        "com.satset.onboarding.model",
                        "com.satset.transaction.model",
                        "com.satset.quickmenu.model",
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
