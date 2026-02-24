package com.omnip.onboarding.domain.port.out;

import com.omnip.onboarding.domain.model.Stores;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for store persistence in onboarding context.
 */
public interface StoreRepositoryPort {

    Stores save(Stores store);

    Optional<Stores> findById(UUID id);

    boolean existsByReferralId(String referralId);
}
