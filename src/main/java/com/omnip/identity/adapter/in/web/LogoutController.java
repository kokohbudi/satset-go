package com.omnip.identity.adapter.in.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class LogoutController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public LogoutController(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("Logout requested for user: {}", auth != null ? auth.getName() : "anonymous");

        // Step 1: Clear all session cookies first
        clearCookies(request, response);

        // Step 2: Invalidate HTTP session
        HttpSession session = request.getSession(false);
        if (session != null) {
            log.debug("Invalidating session: {}", session.getId());
            session.invalidate();
        }

        // Step 3: Clear Spring Security context
        if (auth != null) {
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.setInvalidateHttpSession(true);
            logoutHandler.setClearAuthentication(true);
            logoutHandler.logout(request, response, auth);
        }

        // Step 4: Clear SecurityContextHolder explicitly
        SecurityContextHolder.clearContext();

        // Step 5: Redirect to Keycloak logout (OIDC RP-Initiated Logout)
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutHandler = new OidcClientInitiatedLogoutSuccessHandler(
                clientRegistrationRepository);
        oidcLogoutHandler.setPostLogoutRedirectUri("{baseUrl}/");

        // Trigger OIDC logout which redirects to Keycloak
        oidcLogoutHandler.onLogoutSuccess(request, response, auth);
    }

    /**
     * Clear all relevant cookies to ensure complete logout
     */
    private void clearCookies(HttpServletRequest request, HttpServletResponse response) {
        // List of cookies to clear
        String[] cookiesToClear = { "JSESSIONID", "SESSION", "remember-me" };

        for (String cookieName : cookiesToClear) {
            Cookie cookie = new Cookie(cookieName, null);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }

        // Also clear any cookies from the request
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().contains("SESSION") ||
                        cookie.getName().contains("JSESSIONID") ||
                        cookie.getName().contains("KEYCLOAK")) {
                    Cookie clearCookie = new Cookie(cookie.getName(), null);
                    clearCookie.setPath("/");
                    clearCookie.setMaxAge(0);
                    clearCookie.setHttpOnly(true);
                    response.addCookie(clearCookie);
                    log.debug("Cleared cookie: {}", cookie.getName());
                }
            }
        }
    }
}
