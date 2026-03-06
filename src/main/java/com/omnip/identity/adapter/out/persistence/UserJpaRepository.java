package com.omnip.identity.adapter.out.persistence;

import com.omnip.identity.domain.model.Users;
import com.omnip.identity.domain.port.out.UserRepositoryPort;
import com.omnip.onboarding.domain.port.out.OnboardingUserPort;
import com.omnip.shared.dto.UserDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface untuk entity Users.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data
 * pengguna.
 * Interface ini secara otomatis diimplementasikan oleh Spring Data JPA.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<Users, UUID>, UserRepositoryPort, OnboardingUserPort {

    /**
     * Mencari pengguna berdasarkan alamat email.
     * Hasil pencarian akan disimpan dalam cache untuk mempercepat akses berikutnya.
     *
     * @param email Alamat email pengguna yang dicari
     * @return Objek Users jika ditemukan, null jika tidak ditemukan
     */
    Users findByEmail(String email);

    /**
     * Mencari pengguna berdasarkan alamat email dan mengembalikan sebagai UserDTO.
     * Method ini digunakan oleh shared layer untuk menghindari coupling ke domain model.
     * Implementasi default menggunakan findByEmail() dan konversi ke DTO.
     *
     * @param email Alamat email pengguna yang dicari
     * @return UserDTO jika ditemukan, null jika tidak ditemukan
     */
    default UserDTO findByEmailDTO(String email) {
        Users user = findByEmail(email);
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setFullname(user.getFullname());
        dto.setStoreId(user.getStoreId());
        dto.setRoles(user.getRoles());
        dto.setProviderUserId(user.getProviderUserId());
        dto.setActive(user.isActive());
        return dto;
    }

    /**
     * Mencari store ID berdasarkan provider user ID.
     * Method ini digunakan oleh shared layer untuk menghindari coupling ke domain model.
     * Implementasi default menggunakan findByProviderUserId() dan mengekstrak storeId.
     *
     * @param providerUserId ID pengguna dari provider autentikasi (Keycloak)
     * @return Store UUID jika ditemukan dan user memiliki store, null jika tidak
     */
    default UUID findStoreIdByProviderUserId(String providerUserId) {
        Users user = findByProviderUserId(providerUserId);
        if (user == null) {
            return null;
        }
        return user.getStoreId();
    }

    /**
     * Mencari daftar pengguna berdasarkan email dan store ID.
     * Query ini menggunakan JPQL untuk mendapatkan pengguna yang emailnya terdapat
     * dalam list emails
     * dan memiliki store ID yang cocok.
     *
     * @param emails  List alamat email pengguna yang akan dicari
     * @param storeId ID toko dalam format String (akan dikonversi ke UUID)
     * @return List objek Users yang memenuhi kriteria
     */
    @Query("SELECT u FROM Users u WHERE u.email IN :emails AND u.storeId = CAST(:storeId AS java.util.UUID)")
    List<Users> findByEmailInAndStoreId(@Param("emails") List<String> emails, @Param("storeId") String storeId);

    /**
     * Mencari pengguna berdasarkan provider user ID.
     * Provider user ID biasanya adalah ID dari sistem autentikasi eksternal seperti
     * Keycloak.
     *
     * @param providerUserId ID pengguna dari provider autentikasi
     * @return Objek Users jika ditemukan, null jika tidak ditemukan
     */
    Users findByProviderUserId(String providerUserId);

    /**
     * Mencari pengguna berdasarkan email, username, atau fullname (case
     * insensitive).
     * Digunakan untuk fitur search user dalam role management.
     *
     * @param email    Email yang dicari
     * @param username Username yang dicari
     * @param fullname Fullname yang dicari
     * @return List objek Users yang memenuhi kriteria
     */
    List<Users> findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrFullnameContainingIgnoreCase(
            String email, String username, String fullname);
}