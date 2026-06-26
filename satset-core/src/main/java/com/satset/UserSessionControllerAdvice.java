package com.satset;

import com.satset.identity.client.KeycloakIdentityPort;
import com.satset.quickmenu.service.QuickMenuService;
import com.satset.shared.dto.RoleInfo;
import com.satset.shared.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Controller advice for adding user session attributes to all controllers.
 * Uses shared DTOs (RoleInfo) instead of domain models to avoid coupling
 * shared layer to identity.domain.model package.
 */
@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class UserSessionControllerAdvice {
    private final UserDTO userDTO;
    private final KeycloakIdentityPort keycloakIdentityPort;
    private final QuickMenuService quickMenuService;

    public UserSessionControllerAdvice(UserDTO userDTO,
            KeycloakIdentityPort keycloakIdentityPort,
            QuickMenuService quickMenuService) {
        this.userDTO = userDTO;
        this.keycloakIdentityPort = keycloakIdentityPort;
        this.quickMenuService = quickMenuService;
    }

    @ModelAttribute
    public void addAttributes(Model model, jakarta.servlet.http.HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("user", this.userDTO);
        model.addAttribute("currentPath", request.getRequestURI());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<RoleInfo> roles = (List<RoleInfo>) session.getAttribute("userRoles");
        if (roles == null) {
            String userId = extractUserId(auth);
            if (userId != null) {
                try {
                    roles = keycloakIdentityPort.getMenuRoleInfos(userId);
                    session.setAttribute("userRoles", roles);
                } catch (Exception e) {
                    log.error("Failed to fetch user roles for sidebar", e);
                }
            }
        }
        if (roles != null) {
            model.addAttribute("userRoles", roles);
        }

        String providerUserId = userDTO.getProviderUserId();
        if (providerUserId != null) {
            model.addAttribute("pinnedRoleNames", quickMenuService.pinnedRoleNames(providerUserId));
        }
    }

    private String extractUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        } else if (auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("sub");
        }
        return null;
    }
}
