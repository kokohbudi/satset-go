package com.satset.identity.service.user;

import com.satset.identity.repository.UserRepository;
import com.satset.identity.model.Users;
import com.satset.shared.constant.SatsetConstants;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

/**
 * Service yang menangani operasi manajemen pengguna.
 * Menyediakan metode untuk operasi CRUD pengguna dan manajemen akun.
 */
@Service
public class UserDomainService {
    private final UserRepository usersRepository;
    private final UserManagementHelper userManagementBusiness;
    private final UserDTO userDTO;

    /**
     * Konstruktor dengan dependency injection.
     *
     * @param usersRepository        Repository untuk operasi data pengguna
     * @param userManagementBusiness Komponen bisnis untuk logika manajemen
     *                               pengguna
     * @param userDTO                DTO yang mewakili pengguna saat ini
     */
    public UserDomainService(UserRepository usersRepository, UserManagementHelper userManagementBusiness,
            UserDTO userDTO) {
        this.usersRepository = usersRepository;
        this.userManagementBusiness = userManagementBusiness;
        this.userDTO = userDTO;
    }

    /**
     * Mencari pengguna berdasarkan alamat email dan mengembalikan sebagai UserDTO.
     * Method ini digunakan oleh shared layer untuk menghindari coupling ke domain model.
     *
     * @param email Alamat email pengguna
     * @return UserDTO jika ditemukan, null jika tidak
     */
    public UserDTO findByEmailDTO(String email) {
        return this.usersRepository.findByEmailDTO(email);
    }

    /**
     * Mendapatkan provider user ID dari email.
     * Digunakan oleh IdentityDomainService untuk orchestration.
     */
    public String getProviderUserIdByEmail(String email) throws BusinessException {
        Users user = this.usersRepository.findByEmail(email);
        if (user == null) {
            throw new BusinessException("User tidak ditemukan: " + email);
        }
        return user.getProviderUserId();
    }

    /**
     * Update status user di database (tanpa Keycloak).
     * Digunakan oleh IdentityDomainService untuk orchestration.
     */
    public void updateUserStatusInDb(String email, boolean status) throws BusinessException {
        UserDTO requestedUserDTO = new UserDTO();
        requestedUserDTO.setEmail(email);
        requestedUserDTO.setActive(status);
        this.userManagementBusiness.setUserStatus(this.userDTO, requestedUserDTO, this.usersRepository);
    }

    /**
     * Menyimpan pengguna baru ke database dari UserDTO.
     * Method ini digunakan oleh shared layer untuk menghindari coupling ke domain model.
     *
     * @param userDTO DTO yang berisi data pengguna baru
     * @return UserDTO dengan data pengguna yang telah disimpan
     */
    public UserDTO createNewUserDTO(UserDTO userDTO) {
        Users user = new Users();
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setFullname(userDTO.getFullname());
        user.setRoles(userDTO.getRoles());
        user.setProviderUserId(userDTO.getProviderUserId());
        user.setRegistrationChannel(SatsetConstants.REGISTRATION_CHANNEL_KEYCLOAK);
        user.setActive(true);
        user.setDeleted(false);
        Users savedUser = this.usersRepository.save(user);
        
        UserDTO result = new UserDTO();
        result.setEmail(savedUser.getEmail());
        result.setUsername(savedUser.getUsername());
        result.setFullname(savedUser.getFullname());
        result.setRoles(savedUser.getRoles());
        result.setProviderUserId(savedUser.getProviderUserId());
        result.setStoreId(savedUser.getStoreId());
        result.setActive(savedUser.isActive());
        return result;
    }
}