package com.satset.onboarding.adapter.out.persistence;

import com.satset.onboarding.domain.model.Stores;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Store persistence. Spring Data provides the implementation.
 */
@Repository
public interface StoreRepository extends JpaRepository<Stores, UUID> {

    boolean existsByReferralId(String referralId);

    Stores findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stores s WHERE s.id = :id")
    Optional<Stores> findByIdWithPessimisticLock(@Param("id") UUID id);
}
