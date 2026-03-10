package com.satset.onboarding.domain.service;

import com.satset.identity.domain.model.Users;
import com.satset.onboarding.domain.model.Stores;
import com.satset.onboarding.domain.port.in.CreateStoreUseCase;
import com.satset.onboarding.domain.port.in.SelfOnboardingUseCase;
import com.satset.onboarding.domain.port.out.KeycloakOrganizationPort;
import com.satset.onboarding.domain.port.out.OnboardingUserPort;
import com.satset.onboarding.domain.port.out.StoreRepositoryPort;
import com.satset.shared.exception.BusinessException;
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
    private final CreateStoreUseCase createStoreUseCase;
    private final OnboardingUserPort usersRepository;

    public StoreOnboardingDomainService(KeycloakOrganizationPort keycloakAdminClientService,
            StoreRepositoryPort storeRepository,
                                        OnboardingUserPort usersRepository,
                                        CreateStoreUseCase createStoreUseCase) {
        this.keycloakAdminClientService = keycloakAdminClientService;
        this.storeRepository = storeRepository;
        this.usersRepository = usersRepository;
        this.createStoreUseCase = createStoreUseCase;
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

            store = createStoreUseCase.createNewStore(store);

            // 4. Link User -> Store
            user.setStoreId(store.getId());
            usersRepository.save(user);

            // 5. Set session hasStore = true
            if (RequestContextHolder.getRequestAttributes() != null) {
                RequestContextHolder.currentRequestAttributes()
                        .setAttribute("hasStore", true, RequestAttributes.SCOPE_SESSION);
            }

            log.info("Successfully onboarded store '{}' (orgId: {}) for user '{}'", orgName, orgId, userId);

        } catch (Exception e) {
            log.error("Failed to onboard store, organization '{}' might be orphaned in Keycloak", orgName, e);
            throw new BusinessException("Gagal mendaftarkan toko. Silakan coba lagi.");
        }
    }
}
