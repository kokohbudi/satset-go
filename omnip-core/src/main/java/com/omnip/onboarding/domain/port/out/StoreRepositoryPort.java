package com.omnip.onboarding.domain.port.out;

import com.omnip.onboarding.domain.model.Stores;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for store persistence in onboarding context.
 */
public interface StoreRepositoryPort {

    boolean existsByReferralId(String referralId);

    Optional<Stores> findById(UUID id);

    Stores save(Stores store);

    Stores findByEmail(String email);

    List<Stores> findAll();
}
