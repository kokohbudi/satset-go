package com.omnip.shared.interceptor;

import com.omnip.identity.domain.model.Users;
import com.omnip.identity.domain.port.out.UserRepositoryPort;
import com.omnip.shared.constant.OmniConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class StoreOnboardingInterceptor implements HandlerInterceptor {

    private final UserRepositoryPort usersRepository;

    public StoreOnboardingInterceptor(UserRepositoryPort usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Let Spring Security handle unauthenticated requests
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return true;
        }

        String providerUserId = extractProviderUserId(authentication);
        if (providerUserId == null) {
            log.warn("Could not extract providerUserId from authentication principal");
            return true;
        }

        HttpSession session = request.getSession(true);
        Boolean hasStore = (Boolean) session.getAttribute("hasStore");

        if (hasStore == null) {
            log.debug("Session 'hasStore' is null. Querying database for user: {}", providerUserId);
            Users user = usersRepository.findByProviderUserId(providerUserId);

            if (user != null && user.getStoreId() != null) {
                hasStore = true;
                log.debug("User {} has a store. Setting session hasStore=true", providerUserId);
            } else {
                hasStore = false;
                log.debug("User {} does NOT have a store. Setting session hasStore=false", providerUserId);
            }
            session.setAttribute("hasStore", hasStore);
        }

        if (Boolean.FALSE.equals(hasStore)) {
            // Check if user is a backoffice user by looking at their authorities
            // Backoffice users have specific realm roles and don't need a store
            boolean isBackofficeUser = authentication.getAuthorities().stream()
                    .anyMatch(a -> {
                        String role = a.getAuthority();
                        // Consider as backoffice user if they have ANY realm role
                        // excluding the default Keycloak realm roles
                        return role.startsWith(OmniConstants.ROLE_PREFIX_REALM) &&
                                !role.equals(OmniConstants.ROLE_PREFIX_REALM + "offline_access") &&
                                !role.equals(OmniConstants.ROLE_PREFIX_REALM + "uma_authorization") &&
                                !role.startsWith(OmniConstants.ROLE_PREFIX_REALM + "default-roles-");
                    });

            if (isBackofficeUser) {
                log.debug("User {} is a backoffice user based on realm role. Skipping onboarding redirect.",
                        providerUserId);
                return true;
            }

            log.info("User {} does not have a store. Redirecting to /onboarding from {}", providerUserId,
                    request.getRequestURI());
            response.sendRedirect("/onboarding");
            return false;
        }

        return true;
    }

    private String extractProviderUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (principal instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        return null;
    }
}
