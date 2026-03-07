package com.satset.onboarding.domain.service;

import com.satset.identity.domain.model.Users;
import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.in.AdminOnboardingUseCase;
import com.satset.onboarding.domain.port.out.KeycloakOrganizationPort;
import com.satset.onboarding.domain.port.out.OnboardingUserPort;
import com.satset.onboarding.domain.port.out.StoreRepositoryPort;
import com.satset.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
public class AdminOnboardingDomainService implements AdminOnboardingUseCase {

    private final KeycloakOrganizationPort keycloakAdminClientService;
    private final StoreRepositoryPort storeRepository;
    private final OnboardingUserPort usersRepository;

    public AdminOnboardingDomainService(KeycloakOrganizationPort keycloakAdminClientService,
            StoreRepositoryPort storeRepository,
            OnboardingUserPort usersRepository) {
        this.keycloakAdminClientService = keycloakAdminClientService;
        this.storeRepository = storeRepository;
        this.usersRepository = usersRepository;
    }

    @Transactional
    public void onboardReseller(String username, String email, String orgName, String phone,
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
            String userId = keycloakAdminClientService.createResellerUser(username, username, email);

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
                if (upline != null) {
                    store.setUplineId(upline.getId());
                }
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
            }

            // Link User -> Store
            user.setStoreId(store.getId());
            usersRepository.save(user);

            log.info("Successfully admin-onboarded reseller '{}' for store '{}'", username, orgName);

        } catch (Exception e) {
            log.error("Failed to complete Admin Reseller Onboarding for '{}', org '{}' may be orphaned", username,
                    orgId, e);
            throw new BusinessException("Gagal membuat reseller. Silakan coba lagi.");
        }
    }
}
