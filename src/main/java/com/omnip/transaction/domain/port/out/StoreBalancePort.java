package com.omnip.transaction.domain.port.out;

import com.omnip.onboarding.domain.model.Stores;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for store balance operations — cross-context.
 * Transaction context needs read/write access to store balance data.
 */
public interface StoreBalancePort {

    Optional<Stores> findById(UUID id);

    Stores save(Stores store);

    Optional<Stores> findByIdWithPessimisticLock(UUID id);
}
