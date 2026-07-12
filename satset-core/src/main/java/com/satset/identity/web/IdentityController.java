package com.satset.identity.web;

import com.satset.identity.dto.CreateUserRequest;
import com.satset.identity.model.KeycloakGroup;
import com.satset.identity.model.KeycloakRole;
import com.satset.identity.service.IdentityDomainService;
import com.satset.shared.constant.SatsetConstants;
import com.satset.shared.dto.UserDTO;
import com.satset.shared.exception.BusinessException;
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

    private final IdentityDomainService identityService;

    public IdentityController(IdentityDomainService identityService) {
        this.identityService = identityService;
    }

    // ==================== Roles & Groups ====================

    @GetMapping("/groups")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakGroup>> getGroups() {
        return ResponseEntity.ok(identityService.getGroups());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakRole>> getClientRoles() {
        return ResponseEntity.ok(identityService.getRoles());
    }

    @GetMapping("/roles/scope/{scope}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakRole>> getRolesByScope(
            @PathVariable String scope) {
        return ResponseEntity.ok(identityService.getRolesByScope(scope));
    }

    @GetMapping("/groups/{groupId}/roles")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakRole>> getRolesByGroup(
            @PathVariable String groupId) throws BusinessException {
        return ResponseEntity.ok(identityService.getRolesByGroup(groupId));
    }

    @PostMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<Map<String, String>> assignRoleToGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(identityService.assignRoleToGroup(groupId, roleName));
    }

    @DeleteMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<Map<String, String>> unassignRoleFromGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(identityService.unassignRoleFromGroup(groupId, roleName));
    }

    // ==================== User Management ====================

    @PostMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_ASSIGN_USER_TO_GROUPS + "') and @authz.targetIsNotCurrentUser(#userId)")
    public ResponseEntity<Map<String, String>> assignRoleToUser(
            @PathVariable String userId,
            @PathVariable String roleName) throws BusinessException {
        identityService.assignRoleToUser(userId, roleName);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Role '" + roleName + "' assigned to user"));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_ASSIGN_USER_TO_GROUPS + "') and @authz.targetIsNotCurrentUser(#userId)")
    public ResponseEntity<Map<String, String>> unassignRoleFromUser(
            @PathVariable String userId,
            @PathVariable String roleName) throws BusinessException {
        identityService.unassignRoleFromUser(userId, roleName);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Role '" + roleName + "' removed from user"));
    }

    @PutMapping("/users/password")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_USERS + "')")
    public ResponseEntity<UserDTO> changePassword(@RequestBody UserDTO reqUserDTO) {
        return ResponseEntity.ok(identityService.changePassword(reqUserDTO));
    }

    @PutMapping("/users/{email}/status/{status}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_USERS + "')")
    public ResponseEntity<UserDTO> setUserStatus(
            @PathVariable String email,
            @PathVariable boolean status) {
        return ResponseEntity.ok(identityService.setUserStatus(email, status));
    }

    // ==================== User-Group Assignment ====================

    @GetMapping("/users")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_USERS + "')")
    public ResponseEntity<List<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "100") int maxResults) {
        return ResponseEntity.ok(identityService.getAllUsers(maxResults));
    }

    @PostMapping("/users/{userId}/groups/{groupId}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_ASSIGN_USER_TO_GROUPS + "')")
    public ResponseEntity<Map<String, String>> assignUserToGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        return ResponseEntity.ok(identityService.assignUserToGroup(userId, groupId));
    }

    @DeleteMapping("/users/{userId}/groups/{groupId}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_ASSIGN_USER_TO_GROUPS + "')")
    public ResponseEntity<Map<String, String>> removeUserFromGroup(
            @PathVariable String userId,
            @PathVariable String groupId) {
        return ResponseEntity.ok(identityService.removeUserFromGroup(userId, groupId));
    }

    @GetMapping("/users/{userId}/groups")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_USERS + "')")
    public ResponseEntity<List<KeycloakGroup>> getUserGroups(
            @PathVariable String userId) {
        return ResponseEntity.ok(identityService.getUserGroups(userId));
    }

    @GetMapping("/groups/{groupId}/members")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_USERS + "')")
    public ResponseEntity<List<UserDTO>> getGroupMembers(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "false") boolean recursive) {
        return ResponseEntity.ok(identityService.getGroupMembers(groupId, recursive));
    }

    @GetMapping("/groups/hierarchy")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakGroup>> getGroupsHierarchy() {
        return ResponseEntity.ok(identityService.getGroupsHierarchy());
    }

    @GetMapping("/groups/subgroups")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_ROLES + "')")
    public ResponseEntity<List<KeycloakGroup>> getSubGroups(
            @RequestParam String parentPath) {
        return ResponseEntity.ok(identityService.getSubGroups(parentPath));
    }

    // ==================== Backoffice Users ====================

    @GetMapping("/backoffice/users")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_USERS + "')")
    public ResponseEntity<List<UserDTO>> getBackofficeUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String roleFilter) {

        List<UserDTO> users = identityService.getBackofficeUsers(roleFilter);

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
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_CREATE_USERS + "')")
    public ResponseEntity<UserDTO> createBackofficeUser(@Valid @RequestBody CreateUserRequest request) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(request.getUsername());
        userDTO.setEmail(request.getEmail());
        userDTO.setFullname(request.getFullname());
        userDTO.setPassword(request.getPassword());
        userDTO.setRoles(request.getRoles());

        return ResponseEntity.ok(identityService.createBackofficeUser(userDTO));
    }

    @PutMapping("/backoffice/users/{targetProviderUserId}/status/{status}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_USERS + "') and @authz.targetIsNotCurrentUser(#targetProviderUserId)")
    public ResponseEntity<UserDTO> setBackofficeUserStatus(
            @PathVariable String targetProviderUserId,
            @PathVariable boolean status) {
        return ResponseEntity.ok(identityService.setBackofficeUserStatus(targetProviderUserId, status));
    }
}
