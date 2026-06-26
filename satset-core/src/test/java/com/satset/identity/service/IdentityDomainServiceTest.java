package com.satset.identity.service;

import com.satset.identity.model.GroupMemberInfo;
import com.satset.identity.model.KeycloakRole;
import com.satset.identity.client.KeycloakIdentityPort;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityDomainServiceTest {

    @Mock private KeycloakIdentityPort keycloakPort;
    @Mock private UserDomainService userManagementService;

    @InjectMocks
    private IdentityDomainService service;

    // ==================== createBackofficeUser ====================

    @Test
    void createBackofficeUser_SingleRole_CreatesUserAndReturnsProviderId() throws Exception {
        UserDTO req = buildUserDTO("alice", "alice@mail.com", List.of("manage_users"));
        when(keycloakPort.createBackofficeUser(any(), any(), any(), any(), any()))
                .thenReturn("kc-uuid-alice");

        UserDTO result = service.createBackofficeUser(req);

        assertEquals("kc-uuid-alice", result.getProviderUserId());
        assertEquals("success", result.getStatus());
        verify(keycloakPort).createBackofficeUser("alice", "Alice", "alice@mail.com", "secret", "manage_users");
        verify(keycloakPort, never()).assignRoleToUser(any(), any()); // no additional roles
    }

    @Test
    void createBackofficeUser_MultipleRoles_AssignsAdditionalRoles() throws Exception {
        UserDTO req = buildUserDTO("alice", "alice@mail.com", List.of("manage_users", "view_users", "view_catalog"));
        when(keycloakPort.createBackofficeUser(any(), any(), any(), any(), any()))
                .thenReturn("kc-uuid-alice");

        service.createBackofficeUser(req);

        verify(keycloakPort).assignRoleToUser("kc-uuid-alice", "view_users");
        verify(keycloakPort).assignRoleToUser("kc-uuid-alice", "view_catalog");
    }

    @Test
    void createBackofficeUser_KCThrowsBusinessException_ReturnsFailedDTO() throws Exception {
        UserDTO req = buildUserDTO("alice", "alice@mail.com", List.of("manage_users"));
        when(keycloakPort.createBackofficeUser(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessException("Email sudah terdaftar"));

        UserDTO result = service.createBackofficeUser(req);

        assertEquals("failed", result.getStatus());
        assertNotNull(result.getMessage());
    }

    // ==================== changePassword ====================

    @Test
    void changePassword_Success_ReturnsSuccessStatus() throws Exception {
        UserDTO req = new UserDTO();
        req.setEmail("alice@mail.com");
        req.setPassword("newpassword");

        when(userManagementService.getProviderUserIdByEmail("alice@mail.com")).thenReturn("kc-uuid");

        UserDTO result = service.changePassword(req);

        assertEquals("success", result.getStatus());
        verify(keycloakPort).changeUserPassword("kc-uuid", "newpassword");
    }

    @Test
    void changePassword_UserNotFound_ReturnsFailedDTO() throws Exception {
        UserDTO req = new UserDTO();
        req.setEmail("unknown@mail.com");
        when(userManagementService.getProviderUserIdByEmail(any()))
                .thenThrow(new BusinessException("User not found"));

        UserDTO result = service.changePassword(req);

        assertEquals("failed", result.getStatus());
    }

    // ==================== setUserStatus ====================

    @Test
    void setUserStatus_Success_CallsDbAndKeycloak() throws Exception {
        when(userManagementService.getProviderUserIdByEmail("alice@mail.com")).thenReturn("kc-uuid");

        UserDTO result = service.setUserStatus("alice@mail.com", false);

        assertEquals("success", result.getStatus());
        verify(userManagementService).updateUserStatusInDb("alice@mail.com", false);
        verify(keycloakPort).updateUserStatus("kc-uuid", false);
    }

    @Test
    void setUserStatus_Fails_ReturnsFailedDTO() throws Exception {
        when(userManagementService.getProviderUserIdByEmail(any()))
                .thenThrow(new BusinessException("Not found"));

        UserDTO result = service.setUserStatus("bad@mail.com", true);

        assertEquals("failed", result.getStatus());
    }

    // ==================== setBackofficeUserStatus ====================

    @Test
    void setBackofficeUserStatus_Success_UpdatesKeycloak() throws Exception {
        UserDTO result = service.setBackofficeUserStatus("kc-uuid", true);

        assertEquals("success", result.getStatus());
        verify(keycloakPort).updateUserStatus("kc-uuid", true);
    }

    @Test
    void setBackofficeUserStatus_KCThrows_ReturnsGenericFailure() throws Exception {
        doThrow(new RuntimeException("KC error"))
                .when(keycloakPort).updateUserStatus(any(), anyBoolean());

        UserDTO result = service.setBackofficeUserStatus("kc-uuid", true);

        assertEquals("failed", result.getStatus());
        assertEquals("Gagal mengubah status pengguna. Silakan coba lagi.", result.getMessage());
    }

    // ==================== getBackofficeUsers ====================

    @Test
    void getBackofficeUsers_NoFilter_ReturnsOnlyUsersWithRealmRole() {
        UserDTO withRole = new UserDTO();
        withRole.setRoleDetails(List.of(com.satset.shared.dto.RoleInfo.builder().name("manage_users").build()));
        UserDTO konter = new UserDTO(); // no realm role → excluded
        when(keycloakPort.getUsersWithRolesBatch(100)).thenReturn(List.of(withRole, konter));

        List<UserDTO> result = service.getBackofficeUsers(null);

        assertEquals(1, result.size());
    }

    @Test
    void getBackofficeUsers_WithRoleFilter_ReturnsOnlyMatchingUsers() {
        UserDTO userWithRole = new UserDTO();
        userWithRole.setRoleDetails(List.of(com.satset.shared.dto.RoleInfo.builder().name("manage_users").build()));

        UserDTO userWithoutRole = new UserDTO();
        userWithoutRole.setRoleDetails(List.of(com.satset.shared.dto.RoleInfo.builder().name("other_role").build()));

        when(keycloakPort.getUsersWithRolesBatch(100)).thenReturn(List.of(userWithRole, userWithoutRole));
        when(keycloakPort.getCompositeRoleChildNames("manage_users")).thenReturn(Set.of());

        List<UserDTO> result = service.getBackofficeUsers("manage_users");

        assertEquals(1, result.size());
    }

    @Test
    void getBackofficeUsers_WithAllFilter_ReturnsEveryoneWithRealmRole() {
        UserDTO a = new UserDTO();
        a.setRoleDetails(List.of(com.satset.shared.dto.RoleInfo.builder().name("manage_users").build()));
        UserDTO b = new UserDTO();
        b.setRoleDetails(List.of(com.satset.shared.dto.RoleInfo.builder().name("view_catalog").build()));
        when(keycloakPort.getUsersWithRolesBatch(100)).thenReturn(List.of(a, b, new UserDTO()));

        List<UserDTO> result = service.getBackofficeUsers("all");

        assertEquals(2, result.size());
        verify(keycloakPort, never()).getCompositeRoleChildNames(any());
    }

    // ==================== getGroupMembers ====================

    @Test
    void getGroupMembers_MapsMembersToUserDTO() {
        GroupMemberInfo member = new GroupMemberInfo("kc-uuid", "alice", "Alice", "alice@mail.com", true);
        when(keycloakPort.getGroupMembers("group-1", false)).thenReturn(List.of(member));

        List<UserDTO> result = service.getGroupMembers("group-1");

        assertEquals(1, result.size());
        UserDTO dto = result.getFirst();
        assertEquals("kc-uuid", dto.getProviderUserId());
        assertEquals("alice", dto.getUsername());
        assertEquals("Alice", dto.getFullname());
        assertEquals("alice@mail.com", dto.getEmail());
        assertTrue(dto.isActive());
    }

    // ==================== assignRoleToGroup / unassignRoleFromGroup ====================

    @Test
    void assignRoleToGroup_ReturnsSuccessMap() throws Exception {
        Map<String, String> result = service.assignRoleToGroup("group-1", "manage_users");

        assertEquals("success", result.get("status"));
        verify(keycloakPort).assignRoleToGroup("group-1", "manage_users");
    }

    @Test
    void unassignRoleFromGroup_ReturnsSuccessMap() throws Exception {
        Map<String, String> result = service.unassignRoleFromGroup("group-1", "manage_users");

        assertEquals("success", result.get("status"));
        verify(keycloakPort).unassignRoleFromGroup("group-1", "manage_users");
    }

    // ==================== getAllUsers / getRoles / getRolesByScope ====================

    @Test
    void getAllUsers_DelegatesToPort() {
        List<UserDTO> users = List.of(new UserDTO());
        when(keycloakPort.getAllKeycloakUsers(50)).thenReturn(users);

        List<UserDTO> result = service.getAllUsers(50);

        assertEquals(1, result.size());
    }

    @Test
    void getRoles_DelegatesToPort() {
        List<KeycloakRole> roles = List.of(KeycloakRole.builder().name("manage_users").build());
        when(keycloakPort.getRoles()).thenReturn(roles);

        List<KeycloakRole> result = service.getRoles();

        assertEquals(1, result.size());
    }

    @Test
    void getRolesByScope_DelegatesToPort() {
        when(keycloakPort.getRolesByScope("backoffice")).thenReturn(List.of());

        List<KeycloakRole> result = service.getRolesByScope("backoffice");

        assertTrue(result.isEmpty());
        verify(keycloakPort).getRolesByScope("backoffice");
    }

    @Test
    void getRolesByGroup_DelegatesToPort() throws BusinessException {
        when(keycloakPort.getRolesByGroup("g-1")).thenReturn(List.of());

        service.getRolesByGroup("g-1");

        verify(keycloakPort).getRolesByGroup("g-1");
    }

    // ==================== assignRoleToUser / unassignRoleFromUser ====================

    @Test
    void assignRoleToUser_DelegatesToPort() throws BusinessException {
        doNothing().when(keycloakPort).assignRoleToUser("u-1", "manage_users");

        service.assignRoleToUser("u-1", "manage_users");

        verify(keycloakPort).assignRoleToUser("u-1", "manage_users");
    }

    @Test
    void unassignRoleFromUser_DelegatesToPort() throws BusinessException {
        doNothing().when(keycloakPort).unassignRoleFromUser("u-1", "manage_users");

        service.unassignRoleFromUser("u-1", "manage_users");

        verify(keycloakPort).unassignRoleFromUser("u-1", "manage_users");
    }

    // ==================== assignUserToGroup / removeUserFromGroup ====================

    @Test
    void assignUserToGroup_ReturnsSuccessMap() {
        doNothing().when(keycloakPort).assignUserToGroup("u-1", "g-1");

        Map<String, String> result = service.assignUserToGroup("u-1", "g-1");

        assertEquals("success", result.get("status"));
    }

    @Test
    void removeUserFromGroup_ReturnsSuccessMap() {
        doNothing().when(keycloakPort).removeUserFromGroup("u-1", "g-1");

        Map<String, String> result = service.removeUserFromGroup("u-1", "g-1");

        assertEquals("success", result.get("status"));
    }

    @Test
    void getUserGroups_DelegatesToPort() {
        when(keycloakPort.getUserGroups("u-1")).thenReturn(List.of());

        service.getUserGroups("u-1");

        verify(keycloakPort).getUserGroups("u-1");
    }

    // ==================== getGroupMembers (no-arg) / getGroupsHierarchy / getSubGroups ====================

    @Test
    void getGroupMembers_NoArg_DelegatesToTwoArgVersion() {
        when(keycloakPort.getGroupMembers("g-1", false)).thenReturn(List.of());

        service.getGroupMembers("g-1");

        verify(keycloakPort).getGroupMembers("g-1", false);
    }

    @Test
    void getGroupsHierarchy_DelegatesToPort() {
        when(keycloakPort.getGroupsHierarchy()).thenReturn(List.of());

        service.getGroupsHierarchy();

        verify(keycloakPort).getGroupsHierarchy();
    }

    @Test
    void getSubGroups_DelegatesToPort() {
        when(keycloakPort.getSubGroups("/backoffice")).thenReturn(List.of());

        service.getSubGroups("/backoffice");

        verify(keycloakPort).getSubGroups("/backoffice");
    }

    // ==================== getBackofficeUsers (no-arg) / getBackofficeSubGroups / getRolesForDropdown ====================

    @Test
    void getBackofficeUsers_NoArg_DelegatesToFilteredVersion() {
        when(keycloakPort.getUsersWithRolesBatch(100)).thenReturn(List.of());

        service.getBackofficeUsers();

        verify(keycloakPort).getUsersWithRolesBatch(100);
    }

    @Test
    void getBackofficeSubGroups_MapsRolesToGroupDTO() {
        List<KeycloakRole> roles = List.of(
                KeycloakRole.builder().id("r-1").name("manage_users").build());
        when(keycloakPort.getRoles()).thenReturn(roles);

        var result = service.getBackofficeSubGroups();

        assertEquals(1, result.size());
        assertEquals("manage_users", result.getFirst().getName());
        assertEquals("/manage_users", result.getFirst().getPath());
    }

    @Test
    void getRolesForDropdown_DelegatesToPort() {
        when(keycloakPort.getRolesWithHierarchy()).thenReturn(List.of());

        service.getRolesForDropdown();

        verify(keycloakPort).getRolesWithHierarchy();
    }

    // ==================== Helpers ====================

    private UserDTO buildUserDTO(String username, String email, List<String> roles) {
        UserDTO dto = new UserDTO();
        dto.setUsername(username);
        dto.setFullname("Alice");
        dto.setEmail(email);
        dto.setPassword("secret");
        dto.setRoles(roles);
        return dto;
    }
}
