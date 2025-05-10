package com.omnip.repositories;

import com.omnip.entities.Store;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    boolean existsByReferralId(String referalId);

    @Cacheable(value = "stores", key = "#email", cacheManager = "fastCacheManager")
    Store findByEmail(String email);
}
