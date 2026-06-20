package com.satset.onboarding.client;

import com.satset.identity.model.Users;

/**
 * Output port for user persistence in onboarding context.
 * Cross-context port — Onboarding needs to create/update users
 * owned by the Identity context.
 */
public interface OnboardingUserPort {

    Users findByProviderUserId(String providerUserId);

    Users findByEmail(String email);

    Users save(Users user);
}
