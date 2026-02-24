package com.omnip.transaction.domain.port.out;

import com.omnip.onboarding.domain.model.Stores;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for store balance operations — cross-context.
 * Transaction context needs read/write access to store balance data.
 * Note: save() and findById() are inherited from JpaRepository/CrudRepository.
 */
public interface StoreBalancePort {

    Optional<Stores> findByIdWithPessimisticLock(UUID id);
}
