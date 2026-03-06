package com.omnip.onboarding.adapter.out.persistence;

import com.omnip.onboarding.domain.model.Stores;
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreJpaRepository extends JpaRepository<Stores, UUID>, StoreRepositoryPort {

    boolean existsByReferralId(String referalId);

    Stores findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stores s WHERE s.id = :id")
    Optional<Stores> findByIdWithPessimisticLock(@Param("id") UUID id);
}