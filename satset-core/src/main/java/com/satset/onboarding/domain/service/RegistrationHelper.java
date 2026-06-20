package com.satset.onboarding.domain.service;

import com.satset.identity.domain.model.Users;
import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.out.OnboardingUserPort;
import com.satset.onboarding.adapter.out.persistence.StoreRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kelas komponen bisnis yang menangani logika terkait proses registrasi.
 * Menyediakan metode untuk validasi, persiapan data, dan pembuatan entitas
 * yang diperlukan dalam proses registrasi pengguna dan toko.
 */
@Component
public class RegistrationHelper {
    private final StoreRepository storeRepository;
    private final OnboardingUserPort usersRepository;
    private final SecureRandom random = new SecureRandom();

    /**
     * Konstruktor dengan dependency injection repository.
     *
     * @param storeRepository Repository untuk operasi data Store
     * @param usersRepository Repository untuk operasi data Users
     */
    public RegistrationHelper(StoreRepository storeRepository, OnboardingUserPort usersRepository) {
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
    public Stores prepareNewStore(String email, String fullName) {
        Stores stores = new Stores();
        stores.setName(fullName + "'s Store");
        stores.setActive(true);
        stores.setReferralId(this.generateReferralId(fullName));
        stores.setEmail(email);

        return stores;
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
     * @param stores              Store terkait
     * @return Objek Users yang siap disimpan
     */
    public Users prepareNewStoreUser(String email, String fullName, List<String> roles,
                                     String registrationChannel, String providerUserId, Stores stores) {
        Users user = new Users();
        user.setEmail(email);
        // Logika bisnis: username = email
        user.setUsername(email);
        user.setFullname(fullName);
        user.setStoreId(stores.getId());
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
     * @param stores Store yang telah dibuat
     * @param user   User yang telah dibuat
     * @return Map berisi informasi store dan user
     */
    public Map<String, Object> createRegistrationResponse(Stores stores, Users user) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("store", stores);
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
    private String generateRandomString(int length, SecureRandom random) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }
}