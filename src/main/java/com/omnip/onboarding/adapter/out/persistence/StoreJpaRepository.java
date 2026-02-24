package com.omnip.onboarding.adapter.out.persistence;

import com.omnip.onboarding.domain.model.Stores;
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import com.omnip.transaction.domain.port.out.StoreBalancePort;
import jakarta.persistence.LockModeType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreJpaRepository extends JpaRepository<Stores, UUID>, StoreBalancePort, StoreRepositoryPort {

    boolean existsByReferralId(String referalId);

    @Cacheable(value = "stores", key = "#email", cacheManager = "fastCacheManager")
    Stores findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stores s WHERE s.id = :id")
    Optional<Stores> findByIdWithPessimisticLock(@Param("id") UUID id);
}