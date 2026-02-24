package com.omnip.identity.domain.port.out;

import com.omnip.identity.domain.model.Users;

import java.util.List;

/**
 * Output port for user persistence in identity context.
 */
public interface UserRepositoryPort {

    Users findByEmail(String email);

    Users findByProviderUserId(String providerUserId);

    Users save(Users user);

    List<Users> findByEmailInAndStoreId(List<String> emails, String storeId);
}
