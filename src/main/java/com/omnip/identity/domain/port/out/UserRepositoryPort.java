package com.omnip.identity.domain.port.out;

import com.omnip.identity.domain.model.Users;

import java.util.List;

/**
 * Output port for user persistence in identity context.
 * Note: save() is inherited from JpaRepository/CrudRepository.
 */
public interface UserRepositoryPort {

    Users save(Users user);

    Users findByEmail(String email);

    Users findByProviderUserId(String providerUserId);

    List<Users> findByEmailInAndStoreId(List<String> emails, String storeId);
}
