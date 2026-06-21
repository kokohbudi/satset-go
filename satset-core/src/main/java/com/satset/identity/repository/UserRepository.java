package com.satset.identity.repository;

import com.satset.identity.model.Users;
import com.satset.shared.dto.UserDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * User persistence. Spring Data provides the implementation.
 */
@Repository
public interface UserRepository extends JpaRepository<Users, UUID> {

    Users findByEmail(String email);

    Users findByProviderUserId(String providerUserId);

    @Query("SELECT u FROM Users u WHERE u.email IN :emails AND u.storeId = CAST(:storeId AS java.util.UUID)")
    List<Users> findByEmailInAndStoreId(@Param("emails") List<String> emails, @Param("storeId") String storeId);

    /** DTO convenience for shared layer — avoids exposing entity. */
    default UserDTO findByEmailDTO(String email) {
        Users u = findByEmail(email);
        if (u == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setEmail(u.getEmail());
        dto.setUsername(u.getUsername());
        dto.setFullname(u.getFullname());
        dto.setRoles(u.getRoles());
        dto.setStoreId(u.getStoreId());
        dto.setWalletId(u.getWalletId());
        dto.setProviderUserId(u.getProviderUserId());
        dto.setActive(u.isActive());
        return dto;
    }

    /** Store id lookup for shared layer — avoids exposing entity. */
    default UUID findStoreIdByProviderUserId(String providerUserId) {
        Users u = findByProviderUserId(providerUserId);
        return u != null ? u.getStoreId() : null;
    }
}
