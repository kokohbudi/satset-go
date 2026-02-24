package com.omnip.onboarding.domain.port.out;

import com.omnip.identity.domain.model.Users;

/**
 * Output port for user persistence in onboarding context.
 * Cross-context port — Onboarding needs to create/update users
 * owned by the Identity context.
 */
public interface OnboardingUserPort {

    Users findByProviderUserId(String providerUserId);

    Users save(Users user);
}
