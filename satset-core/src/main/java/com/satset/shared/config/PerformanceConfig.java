package com.satset.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
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
         * Cache manager umur panjang untuk Digiflazz price-list.
         * TTL: 5 jam. DF nge-rate-limit endpoint ini (rc 83) & datanya lag 10-15 menit,
         * jadi cukup 1 hit / 5 jam — flow preview+apply reuse hasil cache.
         * Used for: digiflazzPriceList
         */
        @Bean
        public CacheManager digiflazzCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                                "digiflazzPriceList");
                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .expireAfterWrite(5, TimeUnit.HOURS)
                                .maximumSize(10)
                                .recordStats());
                return cacheManager;
        }

        /**
         * Cache katalog admin TANPA TTL — valid selama tidak ada perubahan.
         * Di-evict eksplisit tiap mutasi katalog (manual CRUD atau sync supplier), jadi tidak
         * pernah stale tanpa harus expire by time. List admin kecil (1 key/cache) jadi murah.
         * Used for: adminCategories, adminProducts, adminActiveDenoms
         */
        @Bean
        public CacheManager catalogCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                                "adminCategories",
                                "adminProducts",
                                "adminActiveDenoms");
                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .maximumSize(100)
                                .recordStats());   // NO expireAfterWrite — evict-on-change only
                return cacheManager;
        }

        /**
         * Standard cache manager untuk data yang jarang berubah.
         * TTL: 30 menit, Max size: 2000 entries.
         * Used for: categoriesAll, categoriesByType, products
         */
        @Bean
        public CacheManager standardCacheManager() {
                CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                                "categoriesAll",
                                "categoriesByType",
                                "products");
                cacheManager.setCaffeine(Caffeine.newBuilder()
                                .expireAfterWrite(30, TimeUnit.MINUTES)
                                .maximumSize(2000)
                                .recordStats());
                return cacheManager;
        }
}
