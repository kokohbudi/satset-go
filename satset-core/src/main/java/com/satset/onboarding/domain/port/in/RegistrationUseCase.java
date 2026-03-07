package com.satset.onboarding.domain.port.in;

import java.util.List;
import java.util.Map;

/**
 * Input port for new store & user registration.
 */
public interface RegistrationUseCase {

    boolean isEmailRegistered(String email);

    Map<String, Object> registerNewStore(String email, String fullName,
            List<String> roles, String registrationChannel, String providerUserId);
}
