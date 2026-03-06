package com.omnip.shared.listener;

import com.omnip.shared.constant.OmniConstants;
import com.omnip.shared.dto.UserDTO;
import com.omnip.identity.domain.model.Users;
import com.omnip.onboarding.domain.service.RegistrationDomainService;
import com.omnip.identity.domain.service.UserDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KeycloakLoginEventListenerTest {

    private static final String TEST_CLIENT_ID = "satsetgo-client";

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private UserDomainService userDomainService;

    @Mock
    private RegistrationDomainService registrationDomainService;

    @Mock
    private OAuth2LoginAuthenticationToken authenticationToken;

    @Mock
    private OidcUser oidcUser;

    @Mock
    private Jwt jwt;

    @Mock
    private OAuth2AccessToken accessToken;

    private KeycloakLoginEventListener listener;
    private MockHttpServletRequest request;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        listener = new KeycloakLoginEventListener(jwtDecoder, userDomainService, registrationDomainService);
        // Inject clientId via reflection (since it's @Value injected in production)
        Field clientIdField = KeycloakLoginEventListener.class.getDeclaredField("clientId");
        clientIdField.setAccessible(true);
        clientIdField.set(listener, TEST_CLIENT_ID);

        request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @BeforeEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ==================== Main Flow Tests ====================

    @Test
    void onAuthenticationSuccess_NewUserWithEmail_CreatesUserAndSession() {
        // Arrange
        String email = "newuser@test.com";
        String username = "newuser";
        String fullName = "New User";
        String providerUserId = "abc-123-xyz";

        setupOidcUser(email, username, fullName, providerUserId);
        setupJwt(providerUserId, List.of());

        when(registrationDomainService.isEmailRegistered(email)).thenReturn(false);

        Users newUser = new Users();
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setFullname(fullName);
        newUser.setProviderUserId(providerUserId);
        newUser.setActive(true);
        newUser.setDeleted(false);

        when(userDomainService.createNewUser(any(Users.class))).thenReturn(newUser);

        // Act
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authenticationToken);
        listener.onAuthenticationSuccess(event);

        // Assert
        ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
        verify(userDomainService).createNewUser(userCaptor.capture());

        Users capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getEmail()).isEqualTo(email);
        assertThat(capturedUser.getUsername()).isEqualTo(username);
        assertThat(capturedUser.getFullname()).isEqualTo(fullName);
        // ProviderUserId is extracted from JWT and set on the newUser before calling createNewUser
        // The newUser object we create in test already has providerUserId set
        // Note: We're checking the captured argument, not the return value
        assertThat(capturedUser.getProviderUserId()).isEqualTo(providerUserId);
        assertThat(capturedUser.getRegistrationChannel()).isEqualTo(OmniConstants.REGISTRATION_CHANNEL_KEYCLOAK);
        assertThat(capturedUser.isActive()).isTrue();
        assertThat(capturedUser.isDeleted()).isFalse();

        // Verify session attribute
        UserDTO userDTO = (UserDTO) session.getAttribute(OmniConstants.SESSION_USER_DTO);
        assertThat(userDTO).isNotNull();
        assertThat(userDTO.getEmail()).isEqualTo(email);
        assertThat(userDTO.getUsername()).isEqualTo(username);
    }

    @Test
    void onAuthenticationSuccess_ExistingUser_UpdatesSession() {
        // Arrange
        String email = "existing@test.com";
        String username = "existinguser";
        String fullName = "Existing User";
        String providerUserId = "def-456-uvw";

        setupOidcUser(email, username, fullName, providerUserId);
        setupJwt(providerUserId, List.of());
        when(registrationDomainService.isEmailRegistered(email)).thenReturn(true);

        Users existingUser = new Users();
        existingUser.setEmail(email);
        existingUser.setUsername(username);
        existingUser.setFullname(fullName);
        existingUser.setProviderUserId(providerUserId);

        when(userDomainService.findByEmail(email)).thenReturn(existingUser);

        // Act
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authenticationToken);
        listener.onAuthenticationSuccess(event);

        // Assert
        verify(userDomainService, never()).createNewUser(any(Users.class));
        verify(userDomainService).findByEmail(email);

        UserDTO userDTO = (UserDTO) session.getAttribute(OmniConstants.SESSION_USER_DTO);
        assertThat(userDTO).isNotNull();
        assertThat(userDTO.getEmail()).isEqualTo(email);
    }

    @Test
    void onAuthenticationSuccess_NoEmail_LogsWarning() {
        // Arrange
        when(authenticationToken.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getEmail()).thenReturn(null);
        when(oidcUser.getAttributes()).thenReturn(Map.of());
        when(authenticationToken.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("mock-token");
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        // Act
        AuthenticationSuccessEvent event = new AuthenticationSuccessEvent(authenticationToken);
        listener.onAuthenticationSuccess(event);

        // Assert
        verify(userDomainService, never()).createNewUser(any(Users.class));
        verify(userDomainService, never()).findByEmail(anyString());
        assertThat(session.getAttribute(OmniConstants.SESSION_USER_DTO)).isNull();
    }

    // ==================== Role Extraction Tests ====================

    @Test
    void extractRolesFromJwt_WithRealmRoles_ReturnsRoles() {
        // Arrange
        List<String> realmRoles = List.of("view_users", "manage_users");
        when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", realmRoles));
        when(jwt.getClaim("resource_access")).thenReturn(Map.of());

        // Act
        List<String> roles = listener.extractRolesFromJwt(jwt);

        // Assert
        assertThat(roles).containsExactlyInAnyOrder("view_users", "manage_users");
    }

    @Test
    void extractRolesFromJwt_WithClientRoles_ReturnsClientRoles() {
        // Arrange
        List<String> clientRoles = List.of("org_owner", "org_member");
        Map<String, Object> resourceAccess = Map.of(
                TEST_CLIENT_ID, Map.of("roles", clientRoles)
        );
        when(jwt.getClaim("realm_access")).thenReturn(Map.of());
        when(jwt.getClaim("resource_access")).thenReturn(resourceAccess);

        // Act
        List<String> roles = listener.extractRolesFromJwt(jwt);

        // Assert - client roles should be extracted since clientId is injected
        assertThat(roles).containsExactlyInAnyOrder("org_owner", "org_member");
    }

    @Test
    void extractRolesFromJwt_WithBothRealmAndClientRoles_ReturnsAllRoles() {
        // Arrange
        List<String> realmRoles = List.of("view_users");
        List<String> clientRoles = List.of("org_owner");
        Map<String, Object> resourceAccess = Map.of(
                TEST_CLIENT_ID, Map.of("roles", clientRoles)
        );
        when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", realmRoles));
        when(jwt.getClaim("resource_access")).thenReturn(resourceAccess);

        // Act
        List<String> roles = listener.extractRolesFromJwt(jwt);

        // Assert - both realm and client roles should be extracted
        assertThat(roles).containsExactlyInAnyOrder("view_users", "org_owner");
    }

    @Test
    void extractRolesFromJwt_NoRoles_ReturnsEmptyList() {
        // Arrange
        when(jwt.getClaim("realm_access")).thenReturn(null);
        when(jwt.getClaim("resource_access")).thenReturn(null);

        // Act
        List<String> roles = listener.extractRolesFromJwt(jwt);

        // Assert
        assertThat(roles).isEmpty();
    }

    @Test
    void extractRolesFromJwt_Exception_ReturnsEmptyList() {
        // Arrange
        when(jwt.getClaim("realm_access")).thenThrow(new RuntimeException("Claim not found"));

        // Act
        List<String> roles = listener.extractRolesFromJwt(jwt);

        // Assert
        assertThat(roles).isEmpty();
    }

    // ==================== Provider User ID Extraction Tests ====================

    @Test
    void extractProviderUserId_FromJwtSub_ReturnsSub() {
        // Arrange
        String expectedSub = "user-123-abc-xyz";
        when(jwt.getClaimAsString("sub")).thenReturn(expectedSub);

        // Act
        String providerUserId = listener.extractProviderUserId(jwt);

        // Assert
        assertThat(providerUserId).isEqualTo(expectedSub);
    }

    @Test
    void extractProviderUserId_NullSub_ReturnsNull() {
        // Arrange
        when(jwt.getClaimAsString("sub")).thenReturn(null);

        // Act
        String providerUserId = listener.extractProviderUserId(jwt);

        // Assert
        assertThat(providerUserId).isNull();
    }

    // ==================== Helper Methods ====================

    private void setupOidcUser(String email, String username, String fullName, String providerUserId) {
        when(authenticationToken.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getEmail()).thenReturn(email);
        when(oidcUser.getAttributes()).thenReturn(Map.of(
                "email", email,
                "preferred_username", username,
                "name", fullName
        ));
        when(oidcUser.getSubject()).thenReturn(providerUserId);
    }

    private void setupJwt(String providerUserId, List<String> roles) {
        when(authenticationToken.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn("mock-token-value");
        when(jwtDecoder.decode("mock-token-value")).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn(providerUserId);
        when(jwt.getClaimAsString("sub")).thenReturn(providerUserId);
        when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", roles));
        when(jwt.getClaim("resource_access")).thenReturn(Map.of());
    }
}
