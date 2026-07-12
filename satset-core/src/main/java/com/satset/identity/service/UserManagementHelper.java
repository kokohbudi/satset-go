package com.satset.identity.service;

import com.satset.identity.repository.UserRepository;
import com.satset.identity.model.Users;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Komponen bisnis yang menangani logika terkait manajemen pengguna.
 * Menyediakan metode untuk validasi, manipulasi data pengguna, dan
 * kontrol akses berdasarkan store pengguna.
 */
@Component
public class UserManagementHelper {
    private final UserRepository usersRepository;

    /**
     * Konstruktor dengan dependency injection.
     *
     * @param usersRepository Repository untuk operasi data pengguna
     */
    public UserManagementHelper(UserRepository usersRepository) {
        this.usersRepository = usersRepository;
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
}