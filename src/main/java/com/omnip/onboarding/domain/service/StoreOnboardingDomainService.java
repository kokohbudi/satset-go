package com.omnip.onboarding.domain.service;

import com.omnip.onboarding.domain.port.out.KeycloakOrganizationPort;

import com.omnip.onboarding.domain.model.Stores;
import com.omnip.identity.domain.model.Users;
import com.omnip.onboarding.domain.port.in.SelfOnboardingUseCase;
import com.omnip.shared.exception.BusinessException;
import com.omnip.onboarding.domain.port.out.StoreRepositoryPort;
import com.omnip.onboarding.domain.port.out.OnboardingUserPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.UUID;

@Service
@Slf4j
public class StoreOnboardingDomainService implements SelfOnboardingUseCase {

    private final KeycloakOrganizationPort keycloakAdminClientService;
    private final StoreRepositoryPort storeRepository;
    private final OnboardingUserPort usersRepository;

    public StoreOnboardingDomainService(KeycloakOrganizationPort keycloakAdminClientService,
            StoreRepositoryPort storeRepository,
            OnboardingUserPort usersRepository) {
        this.keycloakAdminClientService = keycloakAdminClientService;
        this.storeRepository = storeRepository;
        this.usersRepository = usersRepository;
    }

    @Transactional
    public void onboardStore(String userId, String orgName, String phone) throws BusinessException {
        log.info("Starting onboarding for store: {}, user: {}", orgName, userId);

        // 1. Create Keycloak Organization -> get orgId
        String orgId = keycloakAdminClientService.createOrganization(orgName);

        try {
            // 2. Add user to the new Keycloak Organization
            keycloakAdminClientService.addMemberToOrganization(orgId, userId);

            // 2b. Auto-assign client role org_owner
            keycloakAdminClientService.assignClientRoleToUser(userId, "org_owner");

            // Fetch current User entity by providerId (Keycloak userId)
            Users user = usersRepository.findByProviderUserId(userId);
            if (user == null) {
                throw new BusinessException("User entity not found for providerId: " + userId);
            }

            // 3. Create/update Store in DB (isi keycloakOrganizationId, phone)
            Stores store = new Stores();
            store.setName(orgName);
            store.setPhone(phone);
            store.setKeycloakOrganizationId(orgId);
            store.setEmail(user.getEmail());
            // Generate a random 8-character referral ID
            store.setReferralId(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            store.setActive(true);
            store.setDeleted(false);

            store = storeRepository.save(store);

            // 4. Link User -> Store
            user.setStores(store);
            usersRepository.save(user);

            // 5. Set session hasStore = true
            if (RequestContextHolder.getRequestAttributes() != null) {
                RequestContextHolder.currentRequestAttributes()
                        .setAttribute("hasStore", true, RequestAttributes.SCOPE_SESSION);
            }

            log.info("Successfully onboarded store '{}' (orgId: {}) for user '{}'", orgName, orgId, userId);

        } catch (Exception e) {
            log.error("Failed to onboard store, organization '{}' might be orphaned in Keycloak", orgName, e);
            throw new BusinessException("Store onboarding failed: " + e.getMessage());
        }
    }
}
