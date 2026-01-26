package com.omnip.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle logout via GET request.
 * This is needed because Spring Security 7 defaults to POST for logout.
 */
@Controller
public class LogoutController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public LogoutController(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            // Clear the security context and invalidate session
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }

        // Create OIDC logout handler and redirect to Keycloak logout
        OidcClientInitiatedLogoutSuccessHandler logoutHandler = new OidcClientInitiatedLogoutSuccessHandler(
                clientRegistrationRepository);
        logoutHandler.setPostLogoutRedirectUri("{baseUrl}/");

        // Trigger OIDC logout which redirects to Keycloak
        logoutHandler.onLogoutSuccess(request, response, auth);
    }
}
