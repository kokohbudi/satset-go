package com.satset.identity.domain.port.in;

import com.satset.shared.dto.UserDTO;

import java.util.List;

/**
 * Input port for managing backoffice users.
 * Handles user CRUD, status changes, and listing.
 */
public interface ManageBackofficeUsersUseCase {

    List<UserDTO> getAllUsers(int maxResults);

    UserDTO createBackofficeUser(UserDTO reqUserDTO);

    UserDTO setUserStatus(String email, boolean status);

    UserDTO setBackofficeUserStatus(String providerUserId, boolean status);

    List<UserDTO> getBackofficeUsers();

    List<UserDTO> getBackofficeUsers(String roleFilter);
}
