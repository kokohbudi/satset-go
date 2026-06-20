package com.satset.identity.domain.service;

import com.satset.identity.adapter.out.persistence.UserRepository;
import com.satset.identity.domain.model.Users;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Komponen bisnis yang menangani logika terkait manajemen pengguna.
 * Menyediakan metode untuk validasi, manipulasi data pengguna, dan
 * kontrol akses berdasarkan store pengguna.
 */
@Component
public class UserManagementHelper {
    private final UserDTO userDTO;
    private final UserRepository usersRepository;

    /**
     * Konstruktor dengan dependency injection.
     *
     * @param userDTO         UserDTO yang mewakili pengguna saat ini
     * @param usersRepository Repository untuk operasi data pengguna
     */
    public UserManagementHelper(UserDTO userDTO, UserRepository usersRepository) {
        this.userDTO = userDTO;
        this.usersRepository = usersRepository;
    }

    /**
     * Mendapatkan provider user ID untuk operasi perubahan password.
     * Validasi:
     * 1. Self change password → always allowed
     * 2. Change other user → must be in same store
     * 
     * Note: Role check (hasRole('change_password')) should be done at controller
     * level via @PreAuthorize
     *
     * @param sessionUserDTO UserDTO pengguna yang sedang login
     * @param requestUserDTO UserDTO yang berisi data permintaan
     * @return Provider user ID dari pengguna yang passwordnya akan diubah
     * @throws BusinessException Jika pengguna tidak berada di store yang sama
     */
    public String getProviderUseIdChangePassword(UserDTO sessionUserDTO, UserDTO requestUserDTO)
            throws BusinessException {
        // Self change password - always allowed
        if (Objects.isNull(requestUserDTO.getEmail()) ||
                sessionUserDTO.getEmail().equals(requestUserDTO.getEmail())) {
            return sessionUserDTO.getProviderUserId();
        }

        // Change other user's password - must be in same store
        Users user = this.getRequestedUserOnStore(sessionUserDTO, requestUserDTO, this.usersRepository);
        return user.getProviderUserId();
    }

    /**
     * Mengubah status aktif pengguna.
     * Metode ini mengubah status pengguna yang diminta dan menyimpannya ke
     * database.
     *
     * @param sessionUserDTO   UserDTO pengguna yang sedang login
     * @param requestedUserDTO UserDTO yang berisi data permintaan
     * @param usersRepository  Repository untuk operasi data pengguna
     * @throws BusinessException Jika pengguna yang diminta tidak ditemukan
     */
    public void setUserStatus(UserDTO sessionUserDTO, UserDTO requestedUserDTO, UserRepository usersRepository)
            throws BusinessException {
        Users user = this.getRequestedUserOnStore(sessionUserDTO, requestedUserDTO, usersRepository);

        // Hanya ubah status jika perubahan diperlukan
        if (!(user.isActive() && requestedUserDTO.isActive())) {
            user.setActive(requestedUserDTO.isActive());
            usersRepository.save(user);
        }
    }

    /**
     * Mendapatkan pengguna yang diminta pada toko yang sama dengan pengguna sesi.
     * Metode ini melakukan validasi bahwa kedua pengguna berada pada toko yang
     * sama.
     *
     * @param sessionUserDTO  UserDTO pengguna yang sedang login
     * @param requestUserDTO  UserDTO yang berisi data permintaan
     * @param usersRepository Repository untuk operasi data pengguna
     * @return Objek Users dari pengguna yang diminta
     * @throws BusinessException Jika pengguna tidak berada pada toko yang sama
     */
    private Users getRequestedUserOnStore(UserDTO sessionUserDTO, UserDTO requestUserDTO,
            UserRepository usersRepository) throws BusinessException {
        String[] emails = { sessionUserDTO.getEmail(), requestUserDTO.getEmail() };
        List<Users> users = usersRepository.findByEmailInAndStoreId(List.of(emails),
                sessionUserDTO.getStoreId().toString());

        // Validasi bahwa kedua pengguna berada pada toko yang sama
        if (users.size() != 2) {
            throw new BusinessException("Email tidak terdaftar di toko anda");
        }

        // Kembalikan pengguna yang diminta
        return users.stream()
                .filter(user -> user.getEmail().equals(requestUserDTO.getEmail()))
                .findFirst()
                .get();
    }

    /**
     * Membuat objek Users baru berdasarkan data DTO.
     *
     * @param reqUserDTO     DTO yang berisi data pengguna
     * @param providerUserId ID provider dari sistem autentikasi
     * @return Objek Users yang siap disimpan
     */
    public Users createUserObject(UserDTO reqUserDTO, String providerUserId) {
        Users user = new Users();
        user.setEmail(reqUserDTO.getEmail());
        user.setUsername(reqUserDTO.getUsername());
        user.setFullname(reqUserDTO.getFullname());
        user.setRoles(reqUserDTO.getRoles());
        if (this.userDTO.getStoreId() != null) {
            user.setStoreId(this.userDTO.getStoreId());
        }
        user.setProviderUserId(providerUserId);
        user.setRegistrationChannel("omnia");
        return user;
    }

    /**
     * Membuat respons sukses untuk operasi pengguna.
     *
     * @param reqUserDTO    DTO yang berisi data permintaan
     * @param createdUserId ID pengguna yang telah dibuat
     * @return UserDTO dengan status sukses
     */
    public UserDTO createSuccessResponse(UserDTO reqUserDTO, String createdUserId) {
        reqUserDTO.setProviderUserId(createdUserId);
        reqUserDTO.setPassword(null); // Jangan kembalikan password dalam respons
        reqUserDTO.setStatus("success");
        return reqUserDTO;
    }

    /**
     * Membuat respons error untuk operasi pengguna.
     *
     * @param errorMessage Pesan error
     * @return UserDTO dengan status gagal dan pesan error
     */
    public UserDTO createErrorResponse(String errorMessage) {
        UserDTO returnDTO = new UserDTO();
        returnDTO.setStatus("failed");
        returnDTO.setMessage(errorMessage);
        return returnDTO;
    }
}