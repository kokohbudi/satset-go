package com.satset.identity.domain.port.in;

import com.satset.identity.domain.model.Users;

/**
 * Input port for querying user data.
 */
public interface UserQueryUseCase {

    Users findByEmail(String email);

    Users findByProviderUserId(String providerUserId);
}
