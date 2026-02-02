package com.omnip.controllers;

import com.omnip.dtos.KeycloakGroupDTO;
import com.omnip.dtos.KeycloakRoleDTO;
import com.omnip.dtos.UserDTO;
import com.omnip.dtos.requests.CreateUserRequest;
import com.omnip.exceptions.BusinessException;
import com.omnip.services.IdentityManagementService;
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
public class IdentityManagementController {

    private final IdentityManagementService identityManagementService;

    public IdentityManagementController(IdentityManagementService identityManagementService) {
        this.identityManagementService = identityManagementService;
    }

    // ==================== Roles & Groups ====================

    @GetMapping("/groups")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakGroupDTO>> getGroups() {
        return ResponseEntity.ok(identityManagementService.getGroups());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakRoleDTO>> getClientRoles() {
        return ResponseEntity.ok(identityManagementService.getRoles());
    }

    /**
     * Get roles filtered by scope attribute.
     * Use scope=backoffice for backoffice roles, scope=customer for customer roles.
     */
    @GetMapping("/roles/scope/{scope}")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakRoleDTO>> getRolesByScope(
            @PathVariable String scope) {
        return ResponseEntity.ok(identityManagementService.getRolesByScope(scope));
    }

    @GetMapping("/groups/{groupId}/roles")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakRoleDTO>> getRolesByGroup(
            @PathVariable String groupId) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.getRolesByGroup(groupId));
    }

    @PostMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<Map<String, String>> assignRoleToGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.assignRoleToGroup(groupId, roleName));
    }

    @DeleteMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<Map<String, String>> unassignRoleFromGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.unassignRoleFromGroup(groupId, roleName));
    }

    // ==================== User Management ====================

    @PostMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('assign_user_to_groups')")
    public ResponseEntity<Map<String, String>> assignRoleToUser(
            @PathVariable String userId,
            @PathVariable String roleName) throws BusinessException {
        identityManagementService.assignRoleToUser(userId, roleName);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Role '" + roleName + "' assigned to user"));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('assign_user_to_groups')")
    public ResponseEntity<Map<String, String>> unassignRoleFromUser(
            @PathVariable String userId,
            @PathVariable String roleName) throws BusinessException {
        identityManagementService.unassignRoleFromUser(userId, roleName);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Role '" + roleName + "' removed from user"));
    }

    @PutMapping("/users/password")
    @PreAuthorize("hasRole('manage_users')")
    public ResponseEntity<UserDTO> changePassword(@RequestBody UserDTO reqUserDTO) {
        return ResponseEntity.ok(identityManagementService.changePassword(reqUserDTO));
    }

    @PutMapping("/users/{email}/status/{status}")
    @PreAuthorize("hasRole('manage_users')")
    public ResponseEntity<UserDTO> setUserStatus(
            @PathVariable String email,
            @PathVariable boolean status) {
        return ResponseEntity.ok(identityManagementService.setUserStatus(email, status));
    }

    // ==================== User-Group Assignment ====================

    // ==================== User Management ====================

    @GetMapping("/users")
    @PreAuthorize("hasRole('view_users')")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "100") int maxResults) {
        return ResponseEntity.ok(identityManagementService.getAllUsers(maxResults));
    }

    @PostMapping("/users/{userId}/groups/{groupId}")
    @PreAuthorize("hasRole('assign_user_to_groups')")
    public ResponseEntity<Map<String, String>> assignUserToGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        return ResponseEntity.ok(identityManagementService.assignUserToGroup(userId, groupId));
    }

    @DeleteMapping("/users/{userId}/groups/{groupId}")
    @PreAuthorize("hasRole('assign_user_to_groups')")
    public ResponseEntity<Map<String, String>> removeUserFromGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        return ResponseEntity.ok(identityManagementService.removeUserFromGroup(userId, groupId));
    }

    @GetMapping("/users/{userId}/groups")
    @PreAuthorize("hasRole('manage_users')")
    public ResponseEntity<List<KeycloakGroupDTO>> getUserGroups(
            @PathVariable String userId) {
        return ResponseEntity.ok(identityManagementService.getUserGroups(userId));
    }

    @GetMapping("/groups/{groupId}/members")
    @PreAuthorize("hasRole('manage_users')")
    public ResponseEntity<List<UserDTO>> getGroupMembers(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "false") boolean recursive) {
        return ResponseEntity.ok(identityManagementService.getGroupMembers(groupId, recursive));
    }

    @GetMapping("/groups/hierarchy")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakGroupDTO>> getGroupsHierarchy() {
        return ResponseEntity.ok(identityManagementService.getGroupsHierarchy());
    }

    @GetMapping("/groups/subgroups")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakGroupDTO>> getSubGroups(
            @RequestParam String parentPath) {
        return ResponseEntity.ok(identityManagementService.getSubGroups(parentPath));
    }

    // ==================== Role Attributes ====================

    @GetMapping("/roles/{roleName}")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<KeycloakRoleDTO> getRoleWithAttributes(
            @PathVariable String roleName) {
        return ResponseEntity.ok(identityManagementService.getRoleWithAttributes(roleName));
    }

    @PutMapping("/roles/{roleName}/attributes")
    @PreAuthorize("hasRole('manage_roles')")
    public ResponseEntity<Map<String, String>> updateRoleAttributes(
            @PathVariable String roleName,
            @RequestBody Map<String, List<String>> attributes) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.updateRoleAttributes(roleName, attributes));
    }

    // ==================== Backoffice Users (Unified API) ====================

    /**
     * Get all users under /backoffice hierarchy with their groups.
     * Supports optional search (q) and role filter (roleFilter).
     * roleFilter accepts a composite role name - users with ANY child role will be
     * included.
     */
    @GetMapping("/backoffice/users")
    @PreAuthorize("hasRole('view_users')")
    public ResponseEntity<List<UserDTO>> getBackofficeUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String roleFilter) {

        // Get users with role filtering (service handles composite role expansion)
        List<UserDTO> users = identityManagementService.getBackofficeUsers(roleFilter);

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

    /**
     * Create a new backoffice user at realm level.
     * Validates input and creates user in Keycloak.
     */
    @PostMapping("/backoffice/users")
    @PreAuthorize("hasRole('create_users')")
    public ResponseEntity<UserDTO> createBackofficeUser(@Valid @RequestBody CreateUserRequest request) {
        // Convert CreateUserRequest to UserDTO for service layer
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(request.getUsername());
        userDTO.setEmail(request.getEmail());
        userDTO.setFullname(request.getFullname());
        userDTO.setPassword(request.getPassword());
        userDTO.setRoles(request.getRoles());

        return ResponseEntity.ok(identityManagementService.createBackofficeUser(userDTO));
    }

    /**
     * Toggle backoffice user status (activate/deactivate).
     * Uses Keycloak user ID directly to support Sync-on-Login.
     * 
     * Security: User cannot activate/deactivate themselves.
     */
    @PutMapping("/backoffice/users/{targetProviderUserId}/status/{status}")
    @PreAuthorize("hasRole('manage_users') and @authz.targetIsNotCurrentUser(#targetProviderUserId)")
    public ResponseEntity<UserDTO> setBackofficeUserStatus(
            @PathVariable String targetProviderUserId,
            @PathVariable boolean status) {
        return ResponseEntity.ok(identityManagementService.setBackofficeUserStatus(targetProviderUserId, status));
    }
}
