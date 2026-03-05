package com.omnip.identity.adapter.out.keycloak;

import com.omnip.identity.adapter.in.web.dto.KeycloakRoleDTO;
import com.omnip.shared.dto.UserDTO;
import com.omnip.shared.exception.BusinessException;
import com.omnip.shared.testcontainers.KeycloakContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for KeycloakAdminClientService against a real Keycloak container.
 * Container boots once per JVM via KeycloakContainerSupport static initializer.
 */
class KeycloakAdminClientServiceIT extends KeycloakContainerSupport {

    private KeycloakAdminClientService service;

    @BeforeEach
    void setUp() {
        service = new KeycloakAdminClientService(testRealmAdminClient(), new KeycloakHelper());
        ReflectionTestUtils.setField(service, "realm", TEST_REALM);
        ReflectionTestUtils.setField(service, "keycloakServerUrl", KEYCLOAK.getAuthServerUrl());
        ReflectionTestUtils.setField(service, "clientId", TEST_CLIENT_ID);
        ReflectionTestUtils.setField(service, "keycloakClientSecret", TEST_CLIENT_SECRET);
    }

    // ==================== getRoles ====================

    @Test
    void getRoles_ReturnsRealmRoles_ExcludingSystemRoles() {
        List<KeycloakRoleDTO> roles = service.getRoles();

        assertThat(roles).isNotEmpty();
        assertThat(roles).extracting(KeycloakRoleDTO::getName)
                .contains("view_users", "manage_users", "view_catalog", "manage_catalog")
                // roles dari setupTestRealm() yang mirrors production
                .doesNotContain("offline_access", "uma_authorization");
    }

    // ==================== getAllKeycloakUsers ====================

    @Test
    void getAllKeycloakUsers_ReturnsSeededUser() {
        List<UserDTO> users = service.getAllKeycloakUsers(50);

        assertThat(users).isNotEmpty();
        assertThat(users).extracting(UserDTO::getEmail)
                .contains("testuser@example.com");
    }

    // ==================== userExistsByEmail ====================

    @Test
    void userExistsByEmail_ExistingEmail_ReturnsTrue() {
        assertThat(service.userExistsByEmail("testuser@example.com")).isTrue();
    }

    @Test
    void userExistsByEmail_NonExistingEmail_ReturnsFalse() {
        assertThat(service.userExistsByEmail("ghost@example.com")).isFalse();
    }

    // ==================== createBackofficeUser ====================

    @Test
    void createBackofficeUser_ValidData_CreatesUser() throws BusinessException {
        String userId = service.createBackofficeUser(
                "newadmin", "New Admin", "newadmin@example.com", "password123", "view_users");

        assertThat(userId).isNotNull().isNotEmpty();
        assertThat(service.userExistsByEmail("newadmin@example.com")).isTrue();
    }

    @Test
    void createBackofficeUser_DuplicateEmail_ThrowsBusinessException() throws BusinessException {
        service.createBackofficeUser(
                "dupe1", "Dupe User", "dupe@example.com", "password123", "view_users");

        assertThatThrownBy(() ->
                service.createBackofficeUser(
                        "dupe2", "Dupe User 2", "dupe@example.com", "password123", "view_users"))
                .isInstanceOf(BusinessException.class);
    }

    // ==================== verifyUserPassword ====================

    // TODO: Credential verification requires additional test setup (client credentials grant)
    // @Test
    // void verifyUserPassword_CorrectCredentials_ReturnsTrue() {
    //     assertThat(service.verifyUserPassword("testuser@example.com", "password")).isTrue();
    // }

    // @Test
    // void verifyUserPassword_WrongPassword_ReturnsFalse() {
    //     assertThat(service.verifyUserPassword("testuser@example.com", "wrongpass")).isFalse();
    // }
}
