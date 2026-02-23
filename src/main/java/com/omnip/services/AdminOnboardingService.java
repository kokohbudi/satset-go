package com.omnip.services;

import com.omnip.entities.Stores;
import com.omnip.entities.Users;
import com.omnip.exceptions.BusinessException;
import com.omnip.repositories.StoreRepository;
import com.omnip.repositories.UsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AdminOnboardingService {

    private final KeycloakAdminClientService keycloakAdminClientService;
    private final StoreRepository storeRepository;
    private final UsersRepository usersRepository;

    public AdminOnboardingService(KeycloakAdminClientService keycloakAdminClientService,
            StoreRepository storeRepository,
            UsersRepository usersRepository) {
        this.keycloakAdminClientService = keycloakAdminClientService;
        this.storeRepository = storeRepository;
        this.usersRepository = usersRepository;
    }

    @Transactional
    public void onboardReseller(String username, String email, String orgName, String phone, String role,
            String uplineStoreId) throws BusinessException {
        log.info("Admin onboarding reseller: username={}, orgName={}", username, orgName);

        // 0. Check if email already exists in Keycloak realm
        if (keycloakAdminClientService.userExistsByEmail(email)) {
            throw new BusinessException("Email '" + email + "' sudah terdaftar di sistem");
        }

        // 1. Create Keycloak Organization FIRST -> get orgId
        // (avoid orphaned users if org creation fails)
        String orgId = keycloakAdminClientService.createOrganization(orgName);

        try {
            // 2. Create Reseller User in Keycloak -> get userId
            String userId = keycloakAdminClientService.createResellerUser(username, username, email, role);

            // 3. Add user to Organization
            keycloakAdminClientService.addMemberToOrganization(orgId, userId);

            // 3b. Auto-assign client role org_owner
            keycloakAdminClientService.assignClientRoleToUser(userId, "org_owner");

            // 4. Create Store in DB
            Stores store = new Stores();
            store.setName(orgName);
            store.setPhone(phone);
            store.setEmail(email);
            store.setKeycloakOrganizationId(orgId);
            store.setActive(true);
            store.setDeleted(false);
            store.setReferralId(UUID.randomUUID().toString().substring(0, 8).toUpperCase());

            if (uplineStoreId != null && !uplineStoreId.trim().isEmpty()) {
                Stores upline = storeRepository.findById(UUID.fromString(uplineStoreId)).orElse(null);
                store.setUpline(upline);
            }

            store = storeRepository.save(store);

            // 5. Create User Entity in DB
            Users user = usersRepository.findByProviderUserId(userId);
            if (user == null) {
                user = new Users();
                user.setProviderUserId(userId);
                user.setUsername(username);
                user.setFullname(username);
                user.setEmail(email);
                user.setActive(true);
                user.setDeleted(false);
                user.setRegistrationChannel("ADMIN_ONBOARDING");
                if (role != null && !role.trim().isEmpty()) {
                    user.setRoles(List.of(role));
                }
            }

            // Link User -> Store
            user.setStores(store);
            usersRepository.save(user);

            log.info("Successfully admin-onboarded reseller '{}' for store '{}'", username, orgName);

        } catch (Exception e) {
            log.error("Failed to complete Admin Reseller Onboarding for '{}', org '{}' may be orphaned", username,
                    orgId, e);
            throw new BusinessException("Admin Reseller Onboarding failed: " + e.getMessage());
        }
    }
}
