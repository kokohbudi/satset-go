package com.omnip.controllers;

import com.omnip.dtos.KeycloakGroupDTO;
import com.omnip.dtos.KeycloakRoleDTO;
import com.omnip.dtos.UserDTO;
import com.omnip.exceptions.BusinessException;
import com.omnip.services.IdentityManagementService;
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
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakGroupDTO>> getGroups() {
        return ResponseEntity.ok(identityManagementService.getGroups());
    }

    @GetMapping("/roles")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakRoleDTO>> getClientRoles() {
        return ResponseEntity.ok(identityManagementService.getRoles());
    }

    /**
     * Get roles filtered by scope attribute.
     * Use scope=backoffice for backoffice roles, scope=customer for customer roles.
     */
    @GetMapping("/roles/scope/{scope}")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakRoleDTO>> getRolesByScope(
            @PathVariable String scope) {
        return ResponseEntity.ok(identityManagementService.getRolesByScope(scope));
    }

    @GetMapping("/groups/{groupId}/roles")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakRoleDTO>> getRolesByGroup(
            @PathVariable String groupId) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.getRolesByGroup(groupId));
    }

    @PostMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<Map<String, String>> assignRoleToGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.assignRoleToGroup(groupId, roleName));
    }

    @DeleteMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<Map<String, String>> unassignRoleFromGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.unassignRoleFromGroup(groupId, roleName));
    }

    // ==================== User Management ====================

    @PostMapping("/users")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_users')")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO reqUserDTO) {
        return ResponseEntity.ok(identityManagementService.createUser(reqUserDTO));
    }

    @PutMapping("/users/password")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_users')")
    public ResponseEntity<UserDTO> changePassword(@RequestBody UserDTO reqUserDTO) {
        return ResponseEntity.ok(identityManagementService.changePassword(reqUserDTO));
    }

    @PutMapping("/users/{email}/status/{status}")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_users')")
    public ResponseEntity<UserDTO> setUserStatus(
            @PathVariable String email,
            @PathVariable boolean status) {
        return ResponseEntity.ok(identityManagementService.setUserStatus(email, status));
    }

    // ==================== User-Group Assignment ====================

    // ==================== User Management ====================

    @GetMapping("/users")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('view_users')")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "100") int maxResults) {
        return ResponseEntity.ok(identityManagementService.getAllUsers(maxResults));
    }

    @PostMapping("/users/{userId}/groups/{groupId}")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('assign_user_to_groups')")
    public ResponseEntity<Map<String, String>> assignUserToGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        return ResponseEntity.ok(identityManagementService.assignUserToGroup(userId, groupId));
    }

    @DeleteMapping("/users/{userId}/groups/{groupId}")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('assign_user_to_groups')")
    public ResponseEntity<Map<String, String>> removeUserFromGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        return ResponseEntity.ok(identityManagementService.removeUserFromGroup(userId, groupId));
    }

    @GetMapping("/users/{userId}/groups")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_users')")
    public ResponseEntity<List<KeycloakGroupDTO>> getUserGroups(
            @PathVariable String userId) {
        return ResponseEntity.ok(identityManagementService.getUserGroups(userId));
    }

    @GetMapping("/groups/{groupId}/members")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_users')")
    public ResponseEntity<List<UserDTO>> getGroupMembers(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "false") boolean recursive) {
        return ResponseEntity.ok(identityManagementService.getGroupMembers(groupId, recursive));
    }

    @GetMapping("/groups/hierarchy")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakGroupDTO>> getGroupsHierarchy() {
        return ResponseEntity.ok(identityManagementService.getGroupsHierarchy());
    }

    @GetMapping("/groups/subgroups")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakGroupDTO>> getSubGroups(
            @RequestParam String parentPath) {
        return ResponseEntity.ok(identityManagementService.getSubGroups(parentPath));
    }

    // ==================== Role Attributes ====================

    @GetMapping("/roles/{roleName}")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<KeycloakRoleDTO> getRoleWithAttributes(
            @PathVariable String roleName) {
        return ResponseEntity.ok(identityManagementService.getRoleWithAttributes(roleName));
    }

    @PutMapping("/roles/{roleName}/attributes")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('manage_roles')")
    public ResponseEntity<Map<String, String>> updateRoleAttributes(
            @PathVariable String roleName,
            @RequestBody Map<String, List<String>> attributes) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.updateRoleAttributes(roleName, attributes));
    }

    // ==================== Backoffice Users (Unified API) ====================

    /**
     * Get all users under /backoffice hierarchy with their groups.
     * Supports optional search (q) and group filter (groupId).
     */
    @GetMapping("/backoffice/users")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('view_users')")
    public ResponseEntity<List<UserDTO>> getBackofficeUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String groupId) {

        List<UserDTO> users = identityManagementService.getBackofficeUsers();

        // Server-side filtering
        if (q != null && !q.isBlank()) {
            String query = q.toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getFullname() != null && u.getFullname().toLowerCase().contains(query)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(query)) ||
                            (u.getUsername() != null && u.getUsername().toLowerCase().contains(query)))
                    .toList();
        }

        if (groupId != null && !groupId.isBlank()) {
            users = users.stream()
                    .filter(u -> u.getGroups() != null &&
                            u.getGroups().stream().anyMatch(g -> groupId.equals(g.getId())))
                    .toList();
        }

        return ResponseEntity.ok(users);
    }
}
