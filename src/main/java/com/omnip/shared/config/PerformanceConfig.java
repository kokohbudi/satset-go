package com.omnip.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableTransactionManagement
@EnableJpaRepositories
public class PerformanceConfig {

        /**
         * Fast cache manager untuk data yang sering diakses tapi bisa stale.
         * TTL: 5 menit, Max size: 500 entries.
         * Used for: keycloakRoles, keycloakGroups, groupsHierarchy
         */
        @Bean
        @Primary
        public CacheManager fastCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                                "keycloakRoles",
                                "keycloakGroups",
                                "groupsHierarchy",
                                "backofficeSubGroups",
                                "stores",
                                "menus",
                                "roles",
                                "allRoles",
                                "rolesHierarchy");
                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.MINUTES)
                                .maximumSize(500)
                                .recordStats());
                return cacheManager;
        }

        /**
         * Short TTL cache manager untuk data yang sering berubah.
         * TTL: 30 detik, Max size: 200 entries.
         * Used for: backofficeUsers (user list yang bisa berubah sering)
         */
        @Bean
        public CacheManager shortTtlCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                                "backofficeUsers");
                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .expireAfterWrite(30, TimeUnit.SECONDS)
                                .maximumSize(200)
                                .recordStats());
                return cacheManager;
        }

        /**
         * Standard cache manager untuk data yang jarang berubah.
         * TTL: 30 menit, Max size: 2000 entries.
         * Used for: operators, vouchers, prices, categories, products, denoms
         */
        @Bean
        public CacheManager standardCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                                "operators",
                                "vouchers",
                                "purchasePrices",
                                "sellPrices",
                                "categories",
                                "products",
                                "denoms");
                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .expireAfterWrite(30, TimeUnit.MINUTES)
                                .maximumSize(2000)
                                .recordStats());
                return cacheManager;
        }

        @Bean
        @ConditionalOnProperty(name = "spring.main.lazy-initialization", havingValue = "true")
        public Caffeine<Object, Object> caffeineConfig() {
                return Caffeine.newBuilder()
                                .expireAfterWrite(10, TimeUnit.MINUTES)
                                .maximumSize(1000)
                                .recordStats();
        }
}
