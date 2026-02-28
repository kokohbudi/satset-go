package com.omnip.identity.adapter.in.web;

import com.omnip.identity.adapter.in.web.dto.KeycloakGroupDTO;
import com.omnip.identity.adapter.in.web.dto.KeycloakRoleDTO;
import com.omnip.shared.dto.UserDTO;
import com.omnip.identity.adapter.in.web.dto.CreateUserRequest;
import com.omnip.shared.constant.OmniConstants;
import com.omnip.shared.exception.BusinessException;
import com.omnip.identity.domain.port.in.ChangePasswordUseCase;
import com.omnip.identity.domain.port.in.ManageBackofficeUsersUseCase;
import com.omnip.identity.domain.port.in.ManageGroupsUseCase;
import com.omnip.identity.domain.port.in.ManageRolesUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller untuk Identity Management.
 * Menyediakan endpoint untuk mengelola roles, groups, dan users.
 */
@RestController
@RequestMapping("/api/idm/")
public class IdentityController {

    private final ManageGroupsUseCase manageGroupsUseCase;
    private final ManageRolesUseCase manageRolesUseCase;
    private final ManageBackofficeUsersUseCase manageBackofficeUsersUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;

    public IdentityController(ManageGroupsUseCase manageGroupsUseCase,
            ManageRolesUseCase manageRolesUseCase,
            ManageBackofficeUsersUseCase manageBackofficeUsersUseCase,
            ChangePasswordUseCase changePasswordUseCase) {
        this.manageGroupsUseCase = manageGroupsUseCase;
        this.manageRolesUseCase = manageRolesUseCase;
        this.manageBackofficeUsersUseCase = manageBackofficeUsersUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
    }

    // ==================== Roles & Groups ====================

    @GetMapping("/groups")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakGroupDTO>> getGroups() {
        return ResponseEntity.ok(manageGroupsUseCase.getGroups());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakRoleDTO>> getClientRoles() {
        return ResponseEntity.ok(manageRolesUseCase.getRoles());
    }

    @GetMapping("/roles/scope/{scope}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakRoleDTO>> getRolesByScope(
            @PathVariable String scope) {
        return ResponseEntity.ok(manageRolesUseCase.getRolesByScope(scope));
    }

    @GetMapping("/groups/{groupId}/roles")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakRoleDTO>> getRolesByGroup(
            @PathVariable String groupId) throws BusinessException {
        return ResponseEntity.ok(manageRolesUseCase.getRolesByGroup(groupId));
    }

    @PostMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<Map<String, String>> assignRoleToGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(manageRolesUseCase.assignRoleToGroup(groupId, roleName));
    }

    @DeleteMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<Map<String, String>> unassignRoleFromGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(manageRolesUseCase.unassignRoleFromGroup(groupId, roleName));
    }

    // ==================== User Management ====================

    @PostMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_ASSIGN_USER_TO_GROUPS + "') and @authz.targetIsNotCurrentUser(#userId)")
    public ResponseEntity<Map<String, String>> assignRoleToUser(
            @PathVariable String userId,
            @PathVariable String roleName) throws BusinessException {
        manageRolesUseCase.assignRoleToUser(userId, roleName);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Role '" + roleName + "' assigned to user"));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_ASSIGN_USER_TO_GROUPS + "') and @authz.targetIsNotCurrentUser(#userId)")
    public ResponseEntity<Map<String, String>> unassignRoleFromUser(
            @PathVariable String userId,
            @PathVariable String roleName) throws BusinessException {
        manageRolesUseCase.unassignRoleFromUser(userId, roleName);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Role '" + roleName + "' removed from user"));
    }

    @PutMapping("/users/password")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_USERS + "')")
    public ResponseEntity<UserDTO> changePassword(@RequestBody UserDTO reqUserDTO) {
        return ResponseEntity.ok(changePasswordUseCase.changePassword(reqUserDTO));
    }

    @PutMapping("/users/{email}/status/{status}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_USERS + "')")
    public ResponseEntity<UserDTO> setUserStatus(
            @PathVariable String email,
            @PathVariable boolean status) {
        return ResponseEntity.ok(manageBackofficeUsersUseCase.setUserStatus(email, status));
    }

    // ==================== User-Group Assignment ====================

    @GetMapping("/users")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_USERS + "')")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "100") int maxResults) {
        return ResponseEntity.ok(manageBackofficeUsersUseCase.getAllUsers(maxResults));
    }

    @PostMapping("/users/{userId}/groups/{groupId}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_ASSIGN_USER_TO_GROUPS + "')")
    public ResponseEntity<Map<String, String>> assignUserToGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        return ResponseEntity.ok(manageGroupsUseCase.assignUserToGroup(userId, groupId));
    }

    @DeleteMapping("/users/{userId}/groups/{groupId}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_ASSIGN_USER_TO_GROUPS + "')")
    public ResponseEntity<Map<String, String>> removeUserFromGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        return ResponseEntity.ok(manageGroupsUseCase.removeUserFromGroup(userId, groupId));
    }

    @GetMapping("/users/{userId}/groups")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_USERS + "')")
    public ResponseEntity<List<KeycloakGroupDTO>> getUserGroups(
            @PathVariable String userId) {
        return ResponseEntity.ok(manageGroupsUseCase.getUserGroups(userId));
    }

    @GetMapping("/groups/{groupId}/members")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_USERS + "')")
    public ResponseEntity<List<UserDTO>> getGroupMembers(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "false") boolean recursive) {
        return ResponseEntity.ok(manageGroupsUseCase.getGroupMembers(groupId, recursive));
    }

    @GetMapping("/groups/hierarchy")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakGroupDTO>> getGroupsHierarchy() {
        return ResponseEntity.ok(manageGroupsUseCase.getGroupsHierarchy());
    }

    @GetMapping("/groups/subgroups")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakGroupDTO>> getSubGroups(
            @RequestParam String parentPath) {
        return ResponseEntity.ok(manageGroupsUseCase.getSubGroups(parentPath));
    }

    // ==================== Backoffice Users ====================

    @GetMapping("/backoffice/users")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_USERS + "')")
    public ResponseEntity<List<UserDTO>> getBackofficeUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String roleFilter) {

        List<UserDTO> users = manageBackofficeUsersUseCase.getBackofficeUsers(roleFilter);

        // Server-side text search filtering
        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getFullname() != null && u.getFullname().toLowerCase().contains(query)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)) ||
                            (u.getUsername() != null && u.getUsername().toLowerCase().contains(query)))
                    .toList();
        }

        return ResponseEntity.ok(users);
    }

    @PostMapping("/backoffice/users")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_CREATE_USERS + "')")
    public ResponseEntity<UserDTO> createBackofficeUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(request.getUsername());
        userDTO.setEmail(request.getEmail());
        userDTO.setFullname(request.getFullname());
        userDTO.setPassword(request.getPassword());
        userDTO.setRoles(request.getRoles());

        return ResponseEntity.ok(manageBackofficeUsersUseCase.createBackofficeUser(userDTO));
    }

    @PutMapping("/backoffice/users/{targetProviderUserId}/status/{status}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_USERS + "') and @authz.targetIsNotCurrentUser(#targetProviderUserId)")
    public ResponseEntity<UserDTO> setBackofficeUserStatus(
            @PathVariable String targetProviderUserId,
            @PathVariable boolean status) {
        return ResponseEntity.ok(manageBackofficeUsersUseCase.setBackofficeUserStatus(targetProviderUserId, status));
    }
}
