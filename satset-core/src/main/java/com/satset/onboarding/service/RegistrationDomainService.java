package com.satset.onboarding.service;

import com.satset.identity.model.Users;
import com.satset.onboarding.model.Stores;
import com.satset.identity.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RegistrationDomainService {
    private final UserRepository onboardingUserPort;
    private final StoreDomainService storeService;
    private final RegistrationHelper registrationBusiness;

    public RegistrationDomainService(UserRepository onboardingUserPort,
            StoreDomainService storeService,
            RegistrationHelper registrationBusiness) {
        this.onboardingUserPort = onboardingUserPort;
        this.storeService = storeService;
        this.registrationBusiness = registrationBusiness;
    }

    /**
     * Memeriksa apakah email sudah terdaftar
     *
     * @param email Email yang akan diperiksa
     * @return true jika email sudah terdaftar
     */
    public boolean isEmailRegistered(String email) {
        return this.registrationBusiness.isEmailRegistered(email);
    }

    /**
     * Mendaftarkan toko baru beserta usernya
     *
     * @param email               Email untuk toko dan user
     * @param fullName            Nama pemilik
     * @param roles               Peran user
     * @param registrationChannel Channel registrasi
     * @param providerUserId      ID provider (Keycloak)
     * @return Map hasil registrasi
     */
    @Transactional
    public Map<String, Object> registerNewStore(String email, String fullName,
            List<String> roles,
            String registrationChannel,
            String providerUserId) {
        Stores stores = this.registrationBusiness.prepareNewStore(email, fullName);

        stores = this.storeService.createNewStore(stores);

        Users user = this.registrationBusiness.prepareNewStoreUser(
                email, fullName, roles, registrationChannel, providerUserId, stores);

        user = this.onboardingUserPort.save(user);

        return this.registrationBusiness.createRegistrationResponse(stores, user);
    }
}