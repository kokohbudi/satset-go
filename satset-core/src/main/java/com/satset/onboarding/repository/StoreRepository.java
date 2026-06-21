package com.satset.onboarding.repository;

import com.satset.onboarding.model.Stores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Store persistence. Spring Data provides the implementation.
 */
@Repository
public interface StoreRepository extends JpaRepository<Stores, UUID> {

    boolean existsByReferralId(String referralId);

    Stores findByEmail(String email);
}
