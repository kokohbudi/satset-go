package com.satset.identity.domain.port.in;

import com.satset.shared.dto.UserDTO;

/**
 * Input port for password change operations.
 */
public interface ChangePasswordUseCase {

    UserDTO changePassword(UserDTO reqUserDTO);
}
