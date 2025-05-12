package com.omnip.business;

import com.omnip.dtos.UserDTO;
import com.omnip.entities.Users;
import com.omnip.exceptions.BusinessException;
import com.omnip.repositories.UsersRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Komponen bisnis yang menangani logika terkait manajemen pengguna.
 * Menyediakan metode untuk validasi, manipulasi data pengguna, dan
 * kontrol akses berdasarkan peran pengguna.
 */
@Component
public class UserManagementBusiness {
    private final UserDTO userDTO;
    private final UsersRepository usersRepository;

    /**
     * Daftar peran yang diizinkan untuk mengubah password pengguna lain.
     * Nilai diambil dari properti konfigurasi aplikasi.
     */
    @Value("#{'${omnip.allowed-role.change-password}'.split(',')}")
    private List<String> allowedChangePasswordRoles;

    /**
     * Konstruktor dengan dependency injection.
     *
     * @param userDTO         UserDTO yang mewakili pengguna saat ini
     * @param usersRepository Repository untuk operasi data pengguna
     */
    public UserManagementBusiness(UserDTO userDTO, UsersRepository usersRepository) {
        this.userDTO = userDTO;
        this.usersRepository = usersRepository;
    }

    /**
     * Memeriksa apakah pengguna memiliki setidaknya satu peran yang diizinkan.
     * Metode ini memeriksa irisan antara peran pengguna dan peran yang diizinkan.
     *
     * @param userRoles    Daftar peran pengguna
     * @param allowedRoles Daftar peran yang diizinkan
     * @return true jika pengguna memiliki setidaknya satu peran yang diizinkan
     */
    private boolean hasAnyAllowedRole(List<String> userRoles, List<String> allowedRoles) {
        // Periksa apakah ada intersection (irisan) antara role user dan allowed roles
        return userRoles.stream().anyMatch(allowedRoles::contains);
    }

    /**
     * Mendapatkan provider user ID untuk operasi perubahan password.
     * Metode ini melakukan validasi apakah pengguna berhak mengubah password
     * berdasarkan peran pengguna dan konteks permintaan.
     *
     * @param sessionUserDTO UserDTO pengguna yang sedang login
     * @param requestUserDTO UserDTO yang berisi data permintaan
     * @return Provider user ID dari pengguna yang passwordnya akan diubah
     * @throws BusinessException Jika pengguna tidak memiliki hak untuk mengubah password
     */
    public String getProviderUseIdChangePassword(UserDTO sessionUserDTO, UserDTO requestUserDTO) throws BusinessException {
        // Jika tidak ada email yang ditentukan dan pengguna sedang mengubah passwordnya sendiri
        if (Objects.isNull(requestUserDTO.getEmail()) && String.valueOf(sessionUserDTO.getEmail()).equals(this.userDTO.getEmail())) {
            return sessionUserDTO.getProviderUserId();
        }

        // Validasi peran untuk mengubah password pengguna lain
        if (!this.hasAnyAllowedRole(sessionUserDTO.getRoles(), this.allowedChangePasswordRoles)) {
            throw new BusinessException("Anda tidak diijinkan");
        }

        // Dapatkan pengguna berdasarkan email permintaan
        Users user = this.getRequestedUserOnStore(sessionUserDTO, requestUserDTO, this.usersRepository);
        return user.getProviderUserId();
    }

    /**
     * Mengubah status aktif pengguna.
     * Metode ini mengubah status pengguna yang diminta dan menyimpannya ke database.
     *
     * @param sessionUserDTO   UserDTO pengguna yang sedang login
     * @param requestedUserDTO UserDTO yang berisi data permintaan
     * @param usersRepository  Repository untuk operasi data pengguna
     * @throws BusinessException Jika pengguna yang diminta tidak ditemukan
     */
    public void setUserStatus(UserDTO sessionUserDTO, UserDTO requestedUserDTO, UsersRepository usersRepository) throws BusinessException {
        Users user = this.getRequestedUserOnStore(sessionUserDTO, requestedUserDTO, usersRepository);

        // Hanya ubah status jika perubahan diperlukan
        if (!(user.isActive() && requestedUserDTO.isActive())) {
            user.setActive(requestedUserDTO.isActive());
            usersRepository.save(user);
        }
    }

    /**
     * Mendapatkan pengguna yang diminta pada toko yang sama dengan pengguna sesi.
     * Metode ini melakukan validasi bahwa kedua pengguna berada pada toko yang sama.
     *
     * @param sessionUserDTO  UserDTO pengguna yang sedang login
     * @param requestUserDTO  UserDTO yang berisi data permintaan
     * @param usersRepository Repository untuk operasi data pengguna
     * @return Objek Users dari pengguna yang diminta
     * @throws BusinessException Jika pengguna tidak berada pada toko yang sama
     */
    private Users getRequestedUserOnStore(UserDTO sessionUserDTO, UserDTO requestUserDTO, UsersRepository usersRepository) throws BusinessException {
        String[] emails = {sessionUserDTO.getEmail(), requestUserDTO.getEmail()};
        List<Users> users = usersRepository.findByEmailInAndStoreId(List.of(emails), sessionUserDTO.getStore().getId().toString());

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
        user.setStore(this.userDTO.getStore());
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
        reqUserDTO.setPassword(null);  // Jangan kembalikan password dalam respons
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