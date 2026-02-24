package com.omnip.onboarding.domain.port.out;

/**
 * Output port for store persistence in onboarding context.
 * Note: save() and findById() are inherited from JpaRepository/CrudRepository.
 */
public interface StoreRepositoryPort {

    boolean existsByReferralId(String referralId);
}
