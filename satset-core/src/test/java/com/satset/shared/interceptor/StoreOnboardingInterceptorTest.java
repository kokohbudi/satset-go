package com.satset.shared.interceptor;

import com.satset.StoreOnboardingInterceptor;

import com.satset.identity.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StoreOnboardingInterceptorTest {

    @Mock
    private UserRepository usersRepository;

    @InjectMocks
    private StoreOnboardingInterceptor interceptor;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== no authentication ====================

    @Test
    void preHandle_NoAuthentication_ReturnsTrue() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(usersRepository, never()).findStoreIdByProviderUserId(anyString());
    }

    @Test
    void preHandle_AnonymousUser_ReturnsTrue() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    // ==================== JWT without store (redirect) ====================

    @Test
    void preHandle_UserWithoutStore_NotBackoffice_RedirectsToOnboarding() throws Exception {
        setJwtAuth("kc-uuid");
        when(usersRepository.findStoreIdByProviderUserId("kc-uuid")).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals("/onboarding", response.getRedirectedUrl());
    }

    // ==================== JWT with store (pass through) ====================

    @Test
    void preHandle_UserWithStore_ReturnsTrue() throws Exception {
        setJwtAuth("kc-uuid");
        when(usersRepository.findStoreIdByProviderUserId("kc-uuid")).thenReturn(UUID.randomUUID());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    // ==================== session cached ====================

    @Test
    void preHandle_SessionHasStoreTrue_SkipsDbQuery() throws Exception {
        setJwtAuth("kc-uuid");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("hasStore", true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(usersRepository, never()).findStoreIdByProviderUserId(anyString());
    }

    @Test
    void preHandle_SessionHasStoreFalse_BackofficeUser_ReturnsTrue() throws Exception {
        // Backoffice user has ROLE_REALM_manage_users authority
        setJwtAuthWithRoles("kc-uuid", "ROLE_REALM_manage_users");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("hasStore", false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    // ==================== helpers ====================

    private void setJwtAuth(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(Map.of("sub", subject)))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));
    }

    private void setJwtAuthWithRoles(String subject, String... roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(Map.of("sub", subject)))
                .build();
        List<SimpleGrantedAuthority> authorities = List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, authorities));
    }
}
