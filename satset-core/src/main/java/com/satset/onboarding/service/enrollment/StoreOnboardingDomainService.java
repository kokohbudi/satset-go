package com.satset.onboarding.service.enrollment;

import com.satset.onboarding.service.store.StoreDomainService;

import com.satset.identity.model.Users;
import com.satset.onboarding.repository.StoreRepository;
import com.satset.onboarding.model.Stores;
import com.satset.identity.client.KeycloakIdentityPort;
import com.satset.identity.repository.UserRepository;
import com.satset.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.UUID;

@Service
@Slf4j
public class StoreOnboardingDomainService {

    private final KeycloakIdentityPort keycloakAdminClientService;
    private final StoreRepository storeRepository;
    private final StoreDomainService storeService;
    private final UserRepository usersRepository;

    public StoreOnboardingDomainService(KeycloakIdentityPort keycloakAdminClientService,
            StoreRepository storeRepository,
                                        UserRepository usersRepository,
                                        StoreDomainService storeService) {
        this.keycloakAdminClientService = keycloakAdminClientService;
        this.storeRepository = storeRepository;
        this.usersRepository = usersRepository;
        this.storeService = storeService;
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

            store = storeService.createNewStore(store);

            // 4. Link User -> Store
            user.setStoreId(store.getId());
            usersRepository.save(user);

            // 5. Set session hasStore = true; drop stale userRoles cache so the
            // sidebar re-fetches the freshly-assigned org_owner roles on next request.
            // (UserSessionControllerAdvice caches userRoles on first request — before
            // onboarding — so without this the owner sees an empty menu until re-login.)
            if (RequestContextHolder.getRequestAttributes() != null) {
                var requestAttributes = RequestContextHolder.currentRequestAttributes();
                requestAttributes.setAttribute("hasStore", true, RequestAttributes.SCOPE_SESSION);
                requestAttributes.removeAttribute("userRoles", RequestAttributes.SCOPE_SESSION);
            }

            log.info("Successfully onboarded store '{}' (orgId: {}) for user '{}'", orgName, orgId, userId);

        } catch (Exception e) {
            log.error("Failed to onboard store, organization '{}' might be orphaned in Keycloak", orgName, e);
            throw new BusinessException("Gagal mendaftarkan toko. Silakan coba lagi.");
        }
    }
}
