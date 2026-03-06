package com.omnip.shared.web;

import com.omnip.shared.dto.UserDTO;
import com.omnip.identity.domain.model.KeycloakRole;
import com.omnip.identity.domain.port.out.KeycloakIdentityPort;
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

@Slf4j
@ControllerAdvice(annotations = Controller.class)
public class UserSessionControllerAdvice {
    private final UserDTO userDTO;
    private final KeycloakIdentityPort keycloakIdentityPort;

    public UserSessionControllerAdvice(UserDTO userDTO, KeycloakIdentityPort keycloakIdentityPort) {
        this.userDTO = userDTO;
        this.keycloakIdentityPort = keycloakIdentityPort;
    }

    @ModelAttribute
    public void addAttributes(Model model, jakarta.servlet.http.HttpSession session,
            jakarta.servlet.http.HttpServletRequest request) {
        model.addAttribute("user", this.userDTO);
        model.addAttribute("currentPath", request.getRequestURI());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // Check if roles are already cached in session
            @SuppressWarnings("unchecked")
            List<KeycloakRole> cachedRoles = (List<KeycloakRole>) session.getAttribute("userRoles");
            if (cachedRoles != null) {
                model.addAttribute("userRoles", cachedRoles);
                return;
            }

            String userId = null;
            if (auth.getPrincipal() instanceof OidcUser oidcUser) {
                userId = oidcUser.getSubject();
            } else if (auth.getPrincipal() instanceof Jwt jwt) {
                userId = jwt.getClaim("sub");
            }

            if (userId != null) {
                try {
                    List<KeycloakRole> roles = keycloakIdentityPort.getMenuRoles(userId);
                    session.setAttribute("userRoles", roles);
                    model.addAttribute("userRoles", roles);
                } catch (Exception e) {
                    log.error("Failed to fetch user roles for sidebar", e);
                }
            }
        }
    }
}
