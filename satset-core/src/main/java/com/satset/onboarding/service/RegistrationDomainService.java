package com.satset.onboarding.service;

import com.satset.identity.repository.UserRepository;
import com.satset.onboarding.repository.StoreRepository;
import org.springframework.stereotype.Service;

@Service
public class RegistrationDomainService {
    private final UserRepository usersRepository;
    private final StoreRepository storeRepository;

    public RegistrationDomainService(UserRepository usersRepository, StoreRepository storeRepository) {
        this.usersRepository = usersRepository;
        this.storeRepository = storeRepository;
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
}
