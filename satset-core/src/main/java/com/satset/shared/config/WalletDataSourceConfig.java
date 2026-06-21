package com.satset.shared.config;

import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Wallet datasource — backs the wallet schema ({@code satset_wallet}) on its own
 * separate database. In-process now (merged from the former satset-wallet module),
 * but kept on an isolated datasource and transaction manager.
 */
@Configuration
@EnableJpaRepositories(
        basePackages = {"com.satset.wallet.repository"},
        entityManagerFactoryRef = "walletEntityManagerFactory",
        transactionManagerRef = "walletTransactionManager")
public class WalletDataSourceConfig {

    @Bean
    @ConfigurationProperties("wallet.datasource")
    public DataSourceProperties walletDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource walletDataSource(
            @org.springframework.beans.factory.annotation.Qualifier("walletDataSourceProperties")
            DataSourceProperties walletDataSourceProperties) {
        return walletDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean(name = "walletEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean walletEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @org.springframework.beans.factory.annotation.Qualifier("walletDataSource")
            DataSource walletDataSource) {

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProperties.put("hibernate.jdbc.batch_size", "25");
        jpaProperties.put("hibernate.default_schema", "satset_wallet");

        return builder
                .dataSource(walletDataSource)
                .packages("com.satset.wallet.model")
                .persistenceUnit("wallet")
                .properties(jpaProperties)
                .build();
    }

    @Bean(name = "walletTransactionManager")
    public PlatformTransactionManager walletTransactionManager(
            @org.springframework.beans.factory.annotation.Qualifier("walletEntityManagerFactory")
            LocalContainerEntityManagerFactoryBean walletEntityManagerFactory) {
        return new JpaTransactionManager(walletEntityManagerFactory.getObject());
    }
}
