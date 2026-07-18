package com.satset.identity.web;

import com.satset.identity.service.user.UserSelfServiceDomainService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserSelfServiceControllerTest {

    @Mock
    private UserSelfServiceDomainService selfService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new UserSelfServiceController(selfService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static final String VALID_BODY = """
            {"newPassword":"newpass123","confirmPassword":"newpass123","currentPassword":"old"}
            """;

    @Test
    void changePassword_WithJwtPrincipal_ReturnsOk() throws Exception {
        setJwtAuth("kc-uuid", "alice@mail.com");
        doNothing().when(selfService).changeMyPassword(eq("kc-uuid"), eq("alice@mail.com"), any());

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void changePassword_WithJwtPrincipal_ValidationFails_Returns400() throws Exception {
        setJwtAuth("kc-uuid", "alice@mail.com");
        doThrow(new IllegalArgumentException("Password tidak cocok"))
                .when(selfService).changeMyPassword(any(), any(), any());

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void changePassword_UnsupportedPrincipal_Returns401() throws Exception {
        // Set an auth with unsupported principal type (plain String)
        var auth = new UsernamePasswordAuthenticationToken("plain-string", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verify(selfService, never()).changeMyPassword(any(), any(), any());
    }

    // ==================== helpers ====================

    private void setJwtAuth(String subject, String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject(subject)
                .claim("email", email)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));
    }
}
