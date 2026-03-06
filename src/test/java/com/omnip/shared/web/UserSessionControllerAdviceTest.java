package com.omnip.shared.web;

import com.omnip.identity.domain.port.out.KeycloakIdentityPort;
import com.omnip.shared.dto.RoleInfo;
import com.omnip.shared.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserSessionControllerAdviceTest {

    @Mock
    private KeycloakIdentityPort keycloakIdentityPort;

    private UserDTO userDTO = new UserDTO();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    // ==================== no authentication ====================

    @Test
    void addAttributes_NoAuthentication_AddsUserAndPathOnly() throws Exception {
        SecurityContextHolder.clearContext();
        UserSessionControllerAdvice advice = new UserSessionControllerAdvice(userDTO, keycloakIdentityPort);
        Model model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dashboard");
        MockHttpSession session = new MockHttpSession();

        advice.addAttributes(model, session, request);

        assertSame(userDTO, model.getAttribute("user"));
        assertEquals("/dashboard", model.getAttribute("currentPath"));
        assertNull(model.getAttribute("userRoles"));
        verify(keycloakIdentityPort, never()).getMenuRoleInfos(anyString());
    }

    @Test
    void addAttributes_AnonymousUser_SkipsRoleFetch() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        UserSessionControllerAdvice advice = new UserSessionControllerAdvice(userDTO, keycloakIdentityPort);
        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        advice.addAttributes(model, session, new MockHttpServletRequest());

        verify(keycloakIdentityPort, never()).getMenuRoleInfos(anyString());
    }

    // ==================== JWT principal ====================

    @Test
    void addAttributes_JwtAuth_FetchesRolesAndCachesInSession() throws Exception {
        setJwtAuth("kc-uuid");
        List<RoleInfo> roles = List.of(RoleInfo.builder().name("manage_users").build());
        when(keycloakIdentityPort.getMenuRoleInfos("kc-uuid")).thenReturn(roles);

        UserSessionControllerAdvice advice = new UserSessionControllerAdvice(userDTO, keycloakIdentityPort);
        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        advice.addAttributes(model, session, new MockHttpServletRequest());

        assertEquals(roles, model.getAttribute("userRoles"));
        assertEquals(roles, session.getAttribute("userRoles"));
    }

    @Test
    void addAttributes_RolesCachedInSession_SkipsKeycloakCall() throws Exception {
        setJwtAuth("kc-uuid");
        List<RoleInfo> cached = List.of(RoleInfo.builder().name("view_users").build());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userRoles", cached);

        UserSessionControllerAdvice advice = new UserSessionControllerAdvice(userDTO, keycloakIdentityPort);
        Model model = new ExtendedModelMap();

        advice.addAttributes(model, session, new MockHttpServletRequest());

        assertEquals(cached, model.getAttribute("userRoles"));
        verify(keycloakIdentityPort, never()).getMenuRoleInfos(anyString());
    }

    @Test
    void addAttributes_KeycloakThrows_DoesNotCrash() throws Exception {
        setJwtAuth("kc-uuid");
        when(keycloakIdentityPort.getMenuRoleInfos("kc-uuid")).thenThrow(new RuntimeException("KC error"));

        UserSessionControllerAdvice advice = new UserSessionControllerAdvice(userDTO, keycloakIdentityPort);
        Model model = new ExtendedModelMap();
        MockHttpSession session = new MockHttpSession();

        // Should not throw
        assertDoesNotThrow(() -> advice.addAttributes(model, session, new MockHttpServletRequest()));
    }

    // ==================== helpers ====================

    private void setJwtAuth(String subject) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claims(c -> c.putAll(Map.of("sub", subject, "email", "test@mail.com")))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));
    }
}
