package com.omnip.business;

import com.omnip.entities.Store;
import com.omnip.entities.Users;
import com.omnip.repositories.StoreRepository;
import com.omnip.repositories.UsersRepository;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Kelas komponen bisnis yang menangani logika terkait proses registrasi.
 * Menyediakan metode untuk validasi, persiapan data, dan pembuatan entitas
 * yang diperlukan dalam proses registrasi pengguna dan toko.
 */
@Component
public class RegistrationBusiness {
    private final StoreRepository storeRepository;
    private final UsersRepository usersRepository;
    private final Random random = new Random();

    /**
     * Konstruktor dengan dependency injection repository.
     *
     * @param storeRepository Repository untuk operasi data Store
     * @param usersRepository Repository untuk operasi data Users
     */
    public RegistrationBusiness(StoreRepository storeRepository, UsersRepository usersRepository) {
        this.storeRepository = storeRepository;
        this.usersRepository = usersRepository;
    }

    /**
     * Memeriksa apakah email sudah terdaftar di sistem.
     * Email dianggap sudah terdaftar jika ditemukan di tabel Users atau Store.
     *
     * @param email Email yang akan diperiksa
     * @return true jika email sudah terdaftar, false jika belum
     */
    public boolean isEmailRegistered(String email) {
        return this.usersRepository.findByEmail(email) != null || this.storeRepository.findByEmail(email) != null;
    }

    /**
     * Membuat objek Store berdasarkan data registrasi.
     * Store dibuat dalam kondisi aktif dan memiliki referral ID yang unik.
     *
     * @param email    Email untuk store
     * @param fullName Nama pemilik untuk store
     * @return Objek Store yang siap disimpan
     */
    public Store prepareNewStore(String email, String fullName) {
        Store store = new Store();
        store.setName(fullName + "'s Store");
        store.setActive(true);
        store.setReferralId(this.generateReferralId(fullName));
        store.setEmail(email);

        return store;
    }

    /**
     * Membuat objek Users terkait store yang baru dibuat.
     * Mengatur semua properti User sesuai standar bisnis untuk pengguna baru.
     *
     * @param email               Email user
     * @param fullName            Nama lengkap user
     * @param roles               Daftar peran user
     * @param registrationChannel Channel registrasi
     * @param providerUserId      ID provider (Keycloak)
     * @param store               Store terkait
     * @return Objek Users yang siap disimpan
     */
    public Users prepareNewStoreUser(String email, String fullName, List<String> roles,
                                     String registrationChannel, String providerUserId, Store store) {
        Users user = new Users();
        user.setEmail(email);
        // Logika bisnis: username = email
        user.setUsername(email);
        user.setFullname(fullName);
        user.setStore(store);
        // Logika bisnis: user baru selalu aktif
        user.setActive(true);
        user.setRoles(roles);
        user.setRegistrationChannel(registrationChannel);
        user.setProviderUserId(providerUserId);

        return user;
    }

    /**
     * Menyiapkan respons untuk registrasi berhasil.
     * Mengemas informasi store dan user dalam satu Map.
     *
     * @param store Store yang telah dibuat
     * @param user  User yang telah dibuat
     * @return Map berisi informasi store dan user
     */
    public Map<String, Object> createRegistrationResponse(Store store, Users user) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("store", store);
        resultMap.put("user", user);
        return resultMap;
    }

    /**
     * Menghasilkan ID referral unik berdasarkan nama pengguna.
     * ID referral terdiri dari huruf yang diekstrak dari nama pengguna
     * dan digabung dengan angka acak untuk memastikan keunikan.
     *
     * @param fullName Nama lengkap pengguna
     * @return ID referral yang unik
     */
    public String generateReferralId(String fullName) {
        String normalizedName = this.normalizeString(fullName);
        String[] nameParts = normalizedName.split("\\s+");
        StringBuilder referralId = new StringBuilder();

        // Logika bisnis: format ID referral berdasarkan nama
        if (nameParts.length >= 2) {
            referralId.append(nameParts[0].substring(0, 1).toUpperCase());
            String lastName = nameParts[nameParts.length - 1];
            referralId.append(lastName.substring(0, Math.min(5, lastName.length())).toUpperCase());
        } else if (nameParts.length == 1) {
            referralId.append(nameParts[0].substring(0, Math.min(6, nameParts[0].length())).toUpperCase());
        }

        referralId.append(String.format("%04d", this.random.nextInt(10000)));
        String candidateId = referralId.toString();

        // Logika bisnis: pastikan ID referral unik
        int attempt = 0;
        while (this.storeRepository.existsByReferralId(candidateId) && attempt < 10) {
            candidateId = referralId.substring(0, referralId.length() - 4) +
                    String.format("%04d", this.random.nextInt(10000));
            attempt++;
        }

        // Logika bisnis: fallback jika masih konflik setelah beberapa percobaan
        if (this.storeRepository.existsByReferralId(candidateId)) {
            candidateId = referralId.substring(0, referralId.length() - 4) +
                    this.generateRandomString(6, this.random);
        }

        return candidateId;
    }

    /**
     * Menormalkan string dengan menghapus aksen dan karakter khusus.
     *
     * @param input String yang akan dinormalkan
     * @return String yang sudah dinormalkan
     */
    private String normalizeString(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.replaceAll("[^a-zA-Z\\s]", "");
        return normalized.toLowerCase();
    }

    /**
     * Menghasilkan string alfanumerik acak dengan panjang tertentu.
     * Digunakan sebagai fallback saat pembuatan ID referral.
     *
     * @param length Panjang string yang diinginkan
     * @param random Objek Random untuk menghasilkan nilai acak
     * @return String acak dengan panjang yang ditentukan
     */
    private String generateRandomString(int length, Random random) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }
}