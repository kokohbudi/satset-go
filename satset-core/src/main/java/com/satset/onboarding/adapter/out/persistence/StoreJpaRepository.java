package com.satset.onboarding.adapter.out.persistence;

import com.satset.onboarding.adapter.out.persistence.entity.StoreJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreJpaRepository extends JpaRepository<StoreJpaEntity, UUID> {

    boolean existsByReferralId(String referralId);

    StoreJpaEntity findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StoreJpaEntity s WHERE s.id = :id")
    Optional<StoreJpaEntity> findByIdWithPessimisticLock(@Param("id") UUID id);
}