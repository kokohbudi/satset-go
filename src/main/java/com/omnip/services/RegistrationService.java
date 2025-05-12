package com.omnip.services;

import com.omnip.business.RegistrationBusiness;
import com.omnip.entities.Store;
import com.omnip.entities.Users;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RegistrationService {
    private final UserManagementService userManagementService;
    private final StoreService storeService;
    private final RegistrationBusiness registrationBusiness;

    public RegistrationService(UserManagementService userManagementService,
                               StoreService storeService,
                               RegistrationBusiness registrationBusiness) {
        this.userManagementService = userManagementService;
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
        Store store = this.registrationBusiness.prepareNewStore(email, fullName);

        store = this.storeService.createNewStore(store);

        Users user = this.registrationBusiness.prepareNewStoreUser(
                email, fullName, roles, registrationChannel, providerUserId, store);

        user = this.userManagementService.createNewUser(user);

        return this.registrationBusiness.createRegistrationResponse(store, user);
    }
}