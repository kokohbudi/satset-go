package com.omnip.identity.domain.port.in;

import com.omnip.shared.dto.UserDTO;

/**
 * Input port for password change operations.
 */
public interface ChangePasswordUseCase {

    UserDTO changePassword(UserDTO reqUserDTO);
}
