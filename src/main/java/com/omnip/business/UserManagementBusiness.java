package com.omnip.business;

import com.omnip.BusinessException;
import com.omnip.dto.UserDTO;
import com.omnip.entities.Users;
import com.omnip.repositories.UsersRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class UserManagementBusiness {
    private final UserDTO userDTO;

    public UserManagementBusiness(UserDTO userDTO) {
        this.userDTO = userDTO;
    }

    public String getProviderUseIdChangePassword(UserDTO sessionUserDTO, UserDTO requestUserDTO, UsersRepository usersRepository, List<String> allowedRoles) throws BusinessException {
        if (Objects.isNull(requestUserDTO.getEmail()) && String.valueOf(sessionUserDTO.getEmail()).equals(this.userDTO.getEmail())) {
            return sessionUserDTO.getProviderUserId();
        }
        if (!requestUserDTO.getRoles().containsAll(allowedRoles)) {
            throw new BusinessException("Anda tidak diijinkan");
        }
        if (this.isEmailSessionAndRequestInSameStore(sessionUserDTO, requestUserDTO, usersRepository) && sessionUserDTO.getRoles().containsAll(allowedRoles)) {
            Users user = usersRepository.findByEmail(requestUserDTO.getEmail());
            return user.getProviderUserId();
        } else {
            throw new BusinessException("Email yang akan dirubah tidak ditemukan di toko anda");
        }

    }

    private boolean isEmailSessionAndRequestInSameStore(UserDTO sessionUserDTO, UserDTO requestUserDTO, UsersRepository usersRepository) {
        String[] emails = {sessionUserDTO.getEmail(), requestUserDTO.getEmail()};
        List<Users> users = usersRepository.findByEmailInAndStoreId(List.of(emails), sessionUserDTO.getStore().getId().toString());
        if (users.size() != 2) {
            return false;
        }
        return true;
    }
}
