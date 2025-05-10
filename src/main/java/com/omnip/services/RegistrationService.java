package com.omnip.services;

import com.omnip.business.RegistrationBusiness;
import com.omnip.entities.Store;
import com.omnip.entities.Users;
import com.omnip.repositories.StoreRepository;
import com.omnip.repositories.UsersRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class RegistrationService {
    private final UserService userService;
    private final StoreService storeService;
    private final UsersRepository usersRepository;
    private final StoreRepository storeRepository;
    private final RegistrationBusiness registrationBusiness;
    private final Random random = new SecureRandom();

    public RegistrationService(UserService userService, StoreService storeService, UsersRepository usersRepository, StoreRepository storeRepository, RegistrationBusiness registrationBusiness) {
        this.userService = userService;
        this.storeService = storeService;
        this.usersRepository = usersRepository;
        this.storeRepository = storeRepository;
        this.registrationBusiness = registrationBusiness;
    }

    public boolean isEmailRegistered(String email) {
        return this.registrationBusiness.isEmailRegistered(email, this.usersRepository, this.storeRepository);
    }

    public Map<String, Object> registerNewStore(String email, String fullName, List roles, String registationChannel, String providerUserId) {
        Store store = new Store();
        store.setName(fullName + "'s Store");
        store.setActive(true);
        store.setReferralId(this.registrationBusiness.generateReferralId(fullName, this.storeRepository, this.random));
        store.setEmail(email);

        store = this.storeService.createNewStore(store);

        Users user = new Users();
        user.setEmail(email);
        user.setUsername(email);
        user.setFullname(fullName);
        user.setStore(store);
        user.setActive(true);
        user.setRoles(roles);
        user.setRegistrationChannel(registationChannel);
        user.setProviderUserId(providerUserId);

        user = this.userService.createNewUser(user);

        Map resultMap = new HashMap();
        resultMap.put("store", store);
        resultMap.put("user", user);
        return resultMap;
    }
}
