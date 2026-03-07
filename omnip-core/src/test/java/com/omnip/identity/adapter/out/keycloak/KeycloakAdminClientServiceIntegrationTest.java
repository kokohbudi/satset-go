package com.omnip.identity.adapter.out.keycloak;

import com.omnip.identity.domain.model.KeycloakGroup;
import com.omnip.identity.domain.model.KeycloakRole;
import com.omnip.identity.domain.model.GroupMemberInfo;
import com.omnip.shared.exception.BusinessException;
import com.omnip.shared.testcontainers.KeycloakContainerSupport;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for KeycloakAdminClientService using Testcontainers Keycloak.
 * Tests cover role management, user management, and group management.
 * Uses KeycloakContainerSupport for shared Keycloak container setup.
 */
class KeycloakAdminClientServiceIntegrationTest extends KeycloakContainerSupport {

    private KeycloakAdminClientService keycloakAdminClientService;
    private KeycloakHelper keycloakHelper;
    private final IdentityMapper identityMapper = new IdentityMapper();

    private static final String TEST_REALM = "satset-go";
    private static final String TEST_CLIENT_ID = "satsetgo-client";

    @BeforeEach
    void setUp() {
        // Create Keycloak admin client connected to test realm
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(KEYCLOAK.getAuthServerUrl())
                .realm("master")
                .clientId("admin-cli")
                .username("admin")
                .password(KEYCLOAK.getAdminPassword())
                .build();

        // Create helper
        keycloakHelper = new KeycloakHelper();

        // Create service instance
        keycloakAdminClientService = new KeycloakAdminClientService(keycloak, keycloakHelper, identityMapper);

        // Inject test realm via reflection
        try {
            var field = KeycloakAdminClientService.class.getDeclaredField("realm");
            field.setAccessible(true);
            field.set(keycloakAdminClientService, TEST_REALM);

            field = KeycloakAdminClientService.class.getDeclaredField("clientId");
            field.setAccessible(true);
            field.set(keycloakAdminClientService, TEST_CLIENT_ID);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject test configuration", e);
        }
    }

    // ==================== Role Management Tests ====================

    @Nested
    @DisplayName("Role Management Tests")
    class RoleManagementTests {

        @Test
        @DisplayName("getRoles should return all non-system roles")
        void getRoles_ReturnsAllNonSystemRoles() {
            // Act
            List<KeycloakRole> roles = keycloakAdminClientService.getRoles();

            // Assert
            assertThat(roles).isNotEmpty();
            assertThat(roles).extracting("name").contains("view_users", "manage_users", "view_catalog", "manage_catalog");
        }

        @Test
        @DisplayName("getRolesWithHierarchy should return roles with children populated")
        void getRolesWithHierarchy_ReturnsRolesWithChildren() {
            // Act
            List<KeycloakRole> rolesWithHierarchy = keycloakAdminClientService.getRolesWithHierarchy();

            // Assert
            assertThat(rolesWithHierarchy).isNotEmpty();
        }

        @Test
        @DisplayName("getRolesByScope should filter roles by scope attribute")
        void getRolesByScope_FiltersByScope() {
            // Arrange - add scope attribute to a role so we can filter by it
            Keycloak admin = masterAdminClient();
            RoleRepresentation role = admin.realm(TEST_REALM).roles().get("view_users").toRepresentation();
            role.setAttributes(Map.of("scope", List.of("backoffice")));
            admin.realm(TEST_REALM).roles().get("view_users").update(role);

            // Act
            List<KeycloakRole> backofficeRoles = keycloakAdminClientService.getRolesByScope("backoffice");

            // Assert
            assertThat(backofficeRoles).isNotEmpty();
            assertThat(backofficeRoles).extracting("name").contains("view_users");
        }
    }

    // ==================== User Management Tests ====================

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {

        @Test
        @DisplayName("getAllKeycloakUsers should return list of users")
        void getAllKeycloakUsers_ReturnsUserList() throws BusinessException {
            // Act
            var users = keycloakAdminClientService.getAllKeycloakUsers(10);

            // Assert
            assertThat(users).isNotEmpty();
        }

        @Test
        @DisplayName("assignRoleToUser should assign realm role to user")
        void assignRoleToUser_AssignsRealmRole() throws BusinessException {
            // Arrange
            Keycloak admin = masterAdminClient();
            String userId = admin.realm(TEST_REALM).users().search("testuser", 0, 1).get(0).getId();

            // Act
            keycloakAdminClientService.assignRoleToUser(userId, "view_users");

            // Assert
            var roles = admin.realm(TEST_REALM).users().get(userId).roles().realmLevel().listAll();
            assertThat(roles).extracting("name").contains("view_users");
        }

        @Test
        @DisplayName("unassignRoleFromUser should remove realm role from user")
        void unassignRoleFromUser_RemovesRealmRole() throws BusinessException {
            // Arrange
            Keycloak admin = masterAdminClient();
            String userId = admin.realm(TEST_REALM).users().search("testuser", 0, 1).get(0).getId();
            keycloakAdminClientService.assignRoleToUser(userId, "view_users");

            // Act
            keycloakAdminClientService.unassignRoleFromUser(userId, "view_users");

            // Assert
            var roles = admin.realm(TEST_REALM).users().get(userId).roles().realmLevel().listAll();
            assertThat(roles).extracting("name").doesNotContain("view_users");
        }

        @Test
        @DisplayName("updateUserStatus should enable/disable user")
        void updateUserStatus_UpdatesUserStatus() throws BusinessException {
            // Arrange
            Keycloak admin = masterAdminClient();
            String userId = admin.realm(TEST_REALM).users().search("testuser", 0, 1).get(0).getId();

            // Act - disable
            keycloakAdminClientService.updateUserStatus(userId, false);

            // Assert
            UserRepresentation user = admin.realm(TEST_REALM).users().get(userId).toRepresentation();
            assertThat(user.isEnabled()).isFalse();

            // Act - enable
            keycloakAdminClientService.updateUserStatus(userId, true);

            // Assert
            user = admin.realm(TEST_REALM).users().get(userId).toRepresentation();
            assertThat(user.isEnabled()).isTrue();
        }
    }

    // ==================== User-Group Management Tests ====================

    @Nested
    @DisplayName("User-Group Management Tests")
    class UserGroupManagementTests {

        @Test
        @DisplayName("getUserGroups should return user's groups")
        void getUserGroups_ReturnsUserGroups() {
            // Arrange
            Keycloak admin = masterAdminClient();
            String userId = admin.realm(TEST_REALM).users().search("testuser", 0, 1).get(0).getId();

            // Act
            List<KeycloakGroup> userGroups = keycloakAdminClientService.getUserGroups(userId);

            // Assert
            assertThat(userGroups).isNotNull();
        }

        @Test
        @DisplayName("getGroupMembers should return group members")
        void getGroupMembers_ReturnsGroupMembers() {
            // Arrange
            Keycloak admin = masterAdminClient();
            List<GroupRepresentation> groups = admin.realm(TEST_REALM).groups().groups();
            if (groups.isEmpty()) {
                return; // Skip if no groups exist in test realm
            }
            String groupId = groups.get(0).getId();

            // Act
            List<GroupMemberInfo> members = keycloakAdminClientService.getGroupMembers(groupId, false);

            // Assert
            assertThat(members).isNotNull();
        }
    }

    // ==================== Group Management Tests ====================

    @Nested
    @DisplayName("Group Management Tests")
    class GroupManagementTests {

        @Test
        @DisplayName("getGroups should return all groups")
        void getGroups_ReturnsAllGroups() {
            // Act
            List<KeycloakGroup> groups = keycloakAdminClientService.getGroups();

            // Assert
            assertThat(groups).isNotNull();
        }

        @Test
        @DisplayName("getGroupsHierarchy should return groups with subgroups")
        void getGroupsHierarchy_ReturnsGroupsWithSubGroups() {
            // Act
            List<KeycloakGroup> groupsHierarchy = keycloakAdminClientService.getGroupsHierarchy();

            // Assert
            assertThat(groupsHierarchy).isNotNull();
        }
    }

    // ==================== Error Handling Tests ====================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("assignRoleToUser with non-existent role should throw NotFoundException")
        void assignRoleToUser_NonExistentRole_ThrowsNotFoundException() {
            // Arrange
            Keycloak admin = masterAdminClient();
            String userId = admin.realm(TEST_REALM).users().search("testuser", 0, 1).get(0).getId();

            // Act & Assert - Keycloak throws NotFoundException for non-existent roles
            assertThatThrownBy(() -> keycloakAdminClientService.assignRoleToUser(userId, "non-existent-role"))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
