package com.omnip.onboarding.domain.port.in;

import com.omnip.shared.exception.BusinessException;

/**
 * Input port for self-service store onboarding.
 */
public interface SelfOnboardingUseCase {

    void onboardStore(String userId, String orgName, String phone) throws BusinessException;
}
