package com.omnip.identity.adapter.in.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserProfileControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserProfileController())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void showProfilePage_WithFullName_AddsFullNameToModel() throws Exception {
        OidcUser oidcUser = buildOidcUser("Alice Smith", "alice@mail.com");
        setOidcAuth(oidcUser);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/identity/profile"))
                .andExpect(model().attribute("name", "Alice Smith"))
                .andExpect(model().attribute("email", "alice@mail.com"));
    }

    @Test
    void showProfilePage_NoFullName_FallsBackToNameClaim() throws Exception {
        OidcUser oidcUser = buildOidcUserNoFullName("Bob Jones", "bob@mail.com");
        setOidcAuth(oidcUser);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("name", "Bob Jones"));
    }

    // ==================== helpers ====================

    private void setOidcAuth(OidcUser oidcUser) {
        var auth = new UsernamePasswordAuthenticationToken(oidcUser, null, oidcUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private OidcUser buildOidcUser(String fullName, String email) {
        String[] parts = fullName.split(" ", 2);
        OidcIdToken token = OidcIdToken.withTokenValue("token")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("kc-abc")
                .claim("email", email)
                .claim("name", fullName)
                .claim("given_name", parts[0])
                .claim("family_name", parts.length > 1 ? parts[1] : "")
                .build();
        return new DefaultOidcUser(null, token);
    }

    private OidcUser buildOidcUserNoFullName(String nameClaim, String email) {
        // no given_name / family_name → getFullName() returns null
        OidcIdToken token = OidcIdToken.withTokenValue("token")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject("kc-def")
                .claim("email", email)
                .claim("name", nameClaim)
                .build();
        return new DefaultOidcUser(null, token);
    }
}
