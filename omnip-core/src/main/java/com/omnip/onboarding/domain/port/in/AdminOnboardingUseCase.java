package com.omnip.onboarding.domain.port.in;

import com.omnip.shared.exception.BusinessException;

/**
 * Input port for admin-initiated reseller onboarding.
 */
public interface AdminOnboardingUseCase {

    void onboardReseller(String username, String email, String orgName, String phone,
            String uplineStoreId) throws BusinessException;
}
