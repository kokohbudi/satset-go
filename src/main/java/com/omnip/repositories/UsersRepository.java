package com.omnip.repositories;

import com.omnip.entities.Users;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface untuk entity Users.
 * Menyediakan operasi CRUD dasar dan method custom untuk mengakses data pengguna.
 * Interface ini secara otomatis diimplementasikan oleh Spring Data JPA.
 */
@Repository
public interface UsersRepository extends JpaRepository<Users, UUID> {

    /**
     * Mencari pengguna berdasarkan alamat email.
     * Hasil pencarian akan disimpan dalam cache untuk mempercepat akses berikutnya.
     *
     * @param email Alamat email pengguna yang dicari
     * @return Objek Users jika ditemukan, null jika tidak ditemukan
     */
    @Cacheable(value = "stores", key = "#email", cacheManager = "fastCacheManager")
    Users findByEmail(String email);

    /**
     * Mencari daftar pengguna berdasarkan email dan store ID.
     * Query ini menggunakan JPQL untuk mendapatkan pengguna yang emailnya terdapat dalam list emails
     * dan memiliki store ID yang cocok.
     *
     * @param emails  List alamat email pengguna yang akan dicari
     * @param storeId ID toko dalam format String (akan dikonversi ke UUID)
     * @return List objek Users yang memenuhi kriteria
     */
    @Query("SELECT u FROM Users u WHERE u.email IN :emails AND u.stores.id = CAST(:storeId AS java.util.UUID)")
    List<Users> findByEmailInAndStoreId(@Param("emails") List<String> emails, @Param("storeId") String storeId);

    /**
     * Mencari pengguna berdasarkan provider user ID.
     * Provider user ID biasanya adalah ID dari sistem autentikasi eksternal seperti Keycloak.
     *
     * @param providerUserId ID pengguna dari provider autentikasi
     * @return Objek Users jika ditemukan, null jika tidak ditemukan
     */
    Users findByProviderUserId(String providerUserId);

    /**
     * Mencari pengguna berdasarkan email, username, atau fullname (case insensitive).
     * Digunakan untuk fitur search user dalam role management.
     *
     * @param email Email yang dicari
     * @param username Username yang dicari  
     * @param fullname Fullname yang dicari
     * @return List objek Users yang memenuhi kriteria
     */
    List<Users> findByEmailContainingIgnoreCaseOrUsernameContainingIgnoreCaseOrFullnameContainingIgnoreCase(
        String email, String username, String fullname);
}