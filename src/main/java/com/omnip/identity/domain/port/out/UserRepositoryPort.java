package com.omnip.identity.domain.port.out;

import com.omnip.identity.domain.model.Users;
import com.omnip.shared.dto.UserDTO;

import java.util.List;
import java.util.UUID;

/**
 * Output port for user persistence in identity context.
 * Note: save() is inherited from JpaRepository/CrudRepository.
 * 
 * Methods returning UserDTO are provided for shared layer consumption
 * to avoid coupling shared layer to domain models.
 */
public interface UserRepositoryPort {

    Users save(Users user);

    Users findByEmail(String email);

    /**
     * Find user by email and return as shared DTO.
     * Use this method from shared layer components to avoid domain model coupling.
     * 
     * @param email User email
     * @return UserDTO or null if not found
     */
    UserDTO findByEmailDTO(String email);

    Users findByProviderUserId(String providerUserId);

    /**
     * Find store ID by provider user ID.
     * Use this method from shared layer components to avoid domain model coupling.
     * 
     * @param providerUserId Keycloak user ID
     * @return Store UUID or null if user not found or has no store
     */
    UUID findStoreIdByProviderUserId(String providerUserId);

    List<Users> findByEmailInAndStoreId(List<String> emails, String storeId);
}
