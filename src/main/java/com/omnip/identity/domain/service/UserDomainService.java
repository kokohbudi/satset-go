package com.omnip.identity.domain.service;

import com.omnip.identity.domain.service.UserManagementHelper;
import com.omnip.shared.dto.UserDTO;
import com.omnip.identity.domain.model.Users;
import com.omnip.identity.domain.port.in.UserQueryUseCase;
import com.omnip.shared.exception.BusinessException;
import com.omnip.identity.adapter.out.persistence.UserJpaRepository;
import com.omnip.identity.adapter.out.keycloak.KeycloakAdminClientService;
import org.springframework.stereotype.Service;

/**
 * Service yang menangani operasi manajemen pengguna.
 * Menyediakan metode untuk operasi CRUD pengguna dan manajemen akun.
 */
@Service
public class UserDomainService implements UserQueryUseCase {
    private final UserJpaRepository usersRepository;
    private final UserManagementHelper userManagementBusiness;
    private final UserDTO userDTO;
    private final KeycloakAdminClientService keycloakAdminClientService;

    /**
     * Konstruktor dengan dependency injection.
     *
     * @param usersRepository            Repository untuk operasi data pengguna
     * @param userManagementBusiness     Komponen bisnis untuk logika manajemen
     *                                   pengguna
     * @param userDTO                    DTO yang mewakili pengguna saat ini
     * @param keycloakAdminClientService Service untuk interaksi dengan Keycloak
     *                                   Admin API
     */
    public UserDomainService(UserJpaRepository usersRepository, UserManagementHelper userManagementBusiness,
            UserDTO userDTO, KeycloakAdminClientService keycloakAdminClientService) {
        this.usersRepository = usersRepository;
        this.userManagementBusiness = userManagementBusiness;
        this.userDTO = userDTO;
        this.keycloakAdminClientService = keycloakAdminClientService;
    }

    /**
     * Mencari pengguna berdasarkan alamat email.
     *
     * @param email Alamat email pengguna
     * @return Objek Users jika ditemukan, null jika tidak
     */
    public Users findByEmail(String email) {
        return this.usersRepository.findByEmail(email);
    }

    /**
     * Mencari pengguna berdasarkan provider user ID.
     *
     * @param providerUserId ID pengguna dari provider autentikasi
     * @return Objek Users jika ditemukan, null jika tidak
     */
    public Users findByProviderUserId(String providerUserId) {
        return this.usersRepository.findByProviderUserId(providerUserId);
    }

    /**
     * Mendapatkan provider user ID untuk operasi perubahan password.
     * Mendelegasikan ke komponen bisnis untuk validasi dan logika bisnis.
     *
     * @param sessionUserDTO UserDTO pengguna yang sedang login
     * @param requestUserDTO UserDTO yang berisi data permintaan
     * @return Provider user ID dari pengguna yang passwordnya akan diubah
     * @throws BusinessException Jika validasi gagal
     */
    public String getProviderUseIdChangePassword(UserDTO sessionUserDTO, UserDTO requestUserDTO)
            throws BusinessException {
        return this.userManagementBusiness.getProviderUseIdChangePassword(sessionUserDTO, requestUserDTO);
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
     * Mengubah status aktif pengguna.
     * DEPRECATED: Gunakan IdentityManagementService.setUserStatus() untuk flow yang
     * lebih clean.
     *
     * @param requestedUserDTO UserDTO yang berisi data permintaan
     * @throws BusinessException Jika operasi gagal
     */
    @Deprecated
    public void setUserStatus(UserDTO requestedUserDTO) throws BusinessException {
        // Update status di database lokal
        this.userManagementBusiness.setUserStatus(this.userDTO, requestedUserDTO, this.usersRepository);

        // Dapatkan pengguna dan update status di Keycloak
        Users user = this.usersRepository.findByEmail(requestedUserDTO.getEmail());
        this.keycloakAdminClientService.updateUserStatus(user.getProviderUserId(), requestedUserDTO.isActive());
    }

    /**
     * Menyimpan pengguna baru ke database.
     *
     * @param user Objek Users yang akan disimpan
     * @return Objek Users yang telah disimpan
     */
    public Users createNewUser(Users user) {
        return this.usersRepository.save(user);
    }

    /**
     * Menyimpan pengguna baru ke database (tanpa membuat di Keycloak).
     * Digunakan oleh IdentityDomainService untuk orchestration.
     *
     * @param reqUserDTO     DTO yang berisi data pengguna
     * @param providerUserId Provider user ID dari Keycloak
     * @return UserDTO dengan status operasi
     */
    public UserDTO saveUserToDb(UserDTO reqUserDTO, String providerUserId) {
        try {
            Users user = this.userManagementBusiness.createUserObject(reqUserDTO, providerUserId);
            this.createNewUser(user);
            return this.userManagementBusiness.createSuccessResponse(reqUserDTO, providerUserId);
        } catch (Exception e) {
            return this.userManagementBusiness
                    .createErrorResponse("Failed to save user to database: " + e.getMessage());
        }
    }

    /**
     * Membuat pengguna baru di Keycloak dan database lokal.
     * DEPRECATED: Gunakan IdentityManagementService.createUser() untuk flow yang
     * lebih clean.
     *
     * @param reqUserDTO DTO yang berisi data pengguna baru
     * @return UserDTO dengan status operasi
     */
    @Deprecated
    public UserDTO createNewUser(UserDTO reqUserDTO) {
        try {
            // Buat pengguna di Keycloak
            String createdProviderUserId = this.keycloakAdminClientService.createBackofficeUser(
                    reqUserDTO.getUsername(),
                    reqUserDTO.getFullname(),
                    reqUserDTO.getEmail(),
                    reqUserDTO.getPassword(),
                    reqUserDTO.getRoles().getFirst());

            // Buat pengguna di database lokal
            Users user = this.userManagementBusiness.createUserObject(reqUserDTO, createdProviderUserId);
            this.createNewUser(user);

            // Buat respons sukses
            return this.userManagementBusiness.createSuccessResponse(reqUserDTO, createdProviderUserId);
        } catch (BusinessException e) {
            // Buat respons error jika gagal
            return this.userManagementBusiness.createErrorResponse(e.getErrorMessage());
        }
    }

    /**
     * Mengubah password pengguna.
     *
     * @param reqUserDTO DTO yang berisi data permintaan perubahan password
     * @return UserDTO dengan status operasi
     */
    public UserDTO changePassword(UserDTO reqUserDTO) {
        UserDTO userDTOReturn = new UserDTO();
        try {
            // Validasi dan dapatkan provider user ID
            String providerUserId = this.getProviderUseIdChangePassword(this.userDTO, reqUserDTO);

            // Ubah password di Keycloak
            this.keycloakAdminClientService.changeUserPassword(providerUserId, reqUserDTO.getPassword());
            userDTOReturn.setStatus("success");
        } catch (BusinessException e) {
            // Tangani error jika gagal
            userDTOReturn.setStatus("failed");
            userDTOReturn.setMessage(e.getErrorMessage());
        }
        return userDTOReturn;
    }
}