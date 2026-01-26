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
    @PreAuthorize("hasAuthority('GROUP_bo-admin') and hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakGroupDTO>> getGroups() {
        return ResponseEntity.ok(identityManagementService.getGroups());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('GROUP_bo-admin') and hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakRoleDTO>> getClientRoles() {
        return ResponseEntity.ok(identityManagementService.getRoles());
    }

    @GetMapping("/groups/{groupId}/roles")
    @PreAuthorize("hasAuthority('GROUP_bo-admin') and hasRole('manage_roles')")
    public ResponseEntity<List<KeycloakRoleDTO>> getRolesByGroup(
            @PathVariable String groupId) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.getRolesByGroup(groupId));
    }

    @PostMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("hasAuthority('GROUP_bo-admin') and hasRole('manage_roles')")
    public ResponseEntity<Map<String, String>> assignRoleToGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.assignRoleToGroup(groupId, roleName));
    }

    @DeleteMapping("/groups/{groupId}/roles/{roleName}")
    @PreAuthorize("hasAuthority('GROUP_bo-admin') and hasRole('manage_roles')")
    public ResponseEntity<Map<String, String>> unassignRoleFromGroup(
            @PathVariable String groupId,
            @PathVariable String roleName) throws BusinessException {
        return ResponseEntity.ok(identityManagementService.unassignRoleFromGroup(groupId, roleName));
    }

    // ==================== User Management ====================

    @PostMapping("/users")
    @PreAuthorize("hasAuthority('GROUP_bo-admin') and hasRole('manage_users')")
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO reqUserDTO) {
        return ResponseEntity.ok(identityManagementService.createUser(reqUserDTO));
    }

    @PutMapping("/users/password")
    @PreAuthorize("hasAuthority('GROUP_bo-admin') and hasRole('manage_users')")
    public ResponseEntity<UserDTO> changePassword(@RequestBody UserDTO reqUserDTO) {
        return ResponseEntity.ok(identityManagementService.changePassword(reqUserDTO));
    }

    @PutMapping("/users/{email}/status/{status}")
    @PreAuthorize("hasAuthority('GROUP_bo-admin') and hasRole('manage_users')")
    public ResponseEntity<UserDTO> setUserStatus(
            @PathVariable String email,
            @PathVariable boolean status) {
        return ResponseEntity.ok(identityManagementService.setUserStatus(email, status));
    }
}
