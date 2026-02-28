package com.omnip.identity.domain.service;

import com.omnip.identity.adapter.in.web.dto.KeycloakGroupDTO;
import com.omnip.identity.domain.port.out.KeycloakIdentityPort;
import com.omnip.identity.adapter.in.web.dto.KeycloakRoleDTO;
import com.omnip.identity.domain.port.in.ChangePasswordUseCase;
import com.omnip.identity.domain.port.in.ManageBackofficeUsersUseCase;
import com.omnip.identity.domain.port.in.ManageGroupsUseCase;
import com.omnip.identity.domain.port.in.ManageRolesUseCase;
import com.omnip.shared.dto.UserDTO;
import com.omnip.shared.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service untuk Identity Management.
 * Orchestration layer untuk mengelola users, roles, dan groups.
 * Mengkoordinasi antara KeycloakAdminClientService dan UserManagementService.
 */
@Service
@Slf4j
public class IdentityDomainService implements ManageGroupsUseCase, ManageRolesUseCase,
        ManageBackofficeUsersUseCase, ChangePasswordUseCase {

    private final KeycloakIdentityPort keycloakAdminClientService;
    private final UserDomainService userManagementService;

    public IdentityDomainService(
            KeycloakIdentityPort keycloakAdminClientService,
            UserDomainService userManagementService) {
        this.keycloakAdminClientService = keycloakAdminClientService;
        this.userManagementService = userManagementService;
    }

    // ==================== Roles & Groups ====================

    /**
     * Mendapatkan semua groups dari Keycloak.
     */
    public List<KeycloakGroupDTO> getGroups() {
        return keycloakAdminClientService.getGroups();
    }

    /**
     * Mendapatkan semua users dari Keycloak.
     */
    public List<UserDTO> getAllUsers(int maxResults) {
        return keycloakAdminClientService.getAllKeycloakUsers(maxResults);
    }

    /**
     * Mendapatkan semua client roles dari Keycloak.
     */
    public List<KeycloakRoleDTO> getRoles() {
        return keycloakAdminClientService.getRoles();
    }

    /**
     * Mendapatkan roles yang difilter berdasarkan scope.
     * Scope adalah attribute di role: scope=backoffice, scope=customer,
     * scope=shared
     *
     * @param scope Nilai scope untuk filter
     */
    public List<KeycloakRoleDTO> getRolesByScope(String scope) {
        return keycloakAdminClientService.getRolesByScope(scope);
    }

    /**
     * Mendapatkan roles yang di-assign ke suatu group.
     */
    public List<KeycloakRoleDTO> getRolesByGroup(String groupId) throws BusinessException {
        return keycloakAdminClientService.getRolesByGroup(groupId);
    }

    /**
     * Assign role ke group.
     */
    public Map<String, String> assignRoleToGroup(String groupId, String roleName) throws BusinessException {
        keycloakAdminClientService.assignRoleToGroup(groupId, roleName);
        log.info("Role '{}' assigned to group '{}'", roleName, groupId);
        return Map.of(
                "status", "success",
                "message", "Role '" + roleName + "' assigned to group '" + groupId + "'");
    }

    /**
     * Unassign/remove role dari group.
     */
    public Map<String, String> unassignRoleFromGroup(String groupId, String roleName) throws BusinessException {
        keycloakAdminClientService.unassignRoleFromGroup(groupId, roleName);
        log.info("Role '{}' removed from group '{}'", roleName, groupId);
        return Map.of(
                "status", "success",
                "message", "Role '" + roleName + "' removed from group '" + groupId + "'");
    }

    /**
     * Assign realm role to user.
     */
    @CacheEvict(value = "backofficeUsers", cacheManager = "shortTtlCacheManager", allEntries = true)
    public void assignRoleToUser(String userId, String roleName) throws BusinessException {
        keycloakAdminClientService.assignRoleToUser(userId, roleName);
        log.info("Role '{}' assigned to user '{}'", roleName, userId);
    }

    /**
     * Unassign realm role from user.
     */
    @CacheEvict(value = "backofficeUsers", cacheManager = "shortTtlCacheManager", allEntries = true)
    public void unassignRoleFromUser(String userId, String roleName) throws BusinessException {
        keycloakAdminClientService.unassignRoleFromUser(userId, roleName);
        log.info("Role '{}' removed from user '{}'", roleName, userId);
    }

    // ==================== User Management ====================

    /**
     * Membuat akun Backoffice User baru.
     * Orchestration:
     * 1. Buat user di Keycloak (via KeycloakAdminClientService)
     * 2. Sync ke DB local akan dilakukan saat user login pertama kali (Sync on
     * Login)
     * 
     * NOTE: Method ini KHUSUS untuk backoffice user.
     */
    @CacheEvict(value = "backofficeUsers", cacheManager = "shortTtlCacheManager", allEntries = true)
    public UserDTO createBackofficeUser(UserDTO reqUserDTO) {
        try {
            // Step 1: Buat user di Keycloak
            String providerUserId = keycloakAdminClientService.createBackofficeUser(
                    reqUserDTO.getUsername(),
                    reqUserDTO.getFullname(),
                    reqUserDTO.getEmail(),
                    reqUserDTO.getPassword(),
                    reqUserDTO.getRoles().getFirst());
            log.info("User created in Keycloak with providerUserId: {}", providerUserId);

            // Return user data (constructed from request + providerId)
            // Note: Data is NOT saved to local DB yet (Sync on Login)
            reqUserDTO.setProviderUserId(providerUserId);
            reqUserDTO.setStatus("success");

            return reqUserDTO;
        } catch (BusinessException e) {
            log.error("Failed to create user: {}", e.getErrorMessage());
            UserDTO errorResult = new UserDTO();
            errorResult.setStatus("failed");
            errorResult.setMessage(e.getErrorMessage());
            return errorResult;
        }
    }

    /**
     * Mengubah password pengguna.
     * Orchestration:
     * 1. Dapatkan provider user ID dari database
     * 2. Ubah password di Keycloak
     */
    public UserDTO changePassword(UserDTO reqUserDTO) {
        UserDTO result = new UserDTO();
        result.setEmail(reqUserDTO.getEmail());
        try {
            // Step 1: Dapatkan provider user ID dari database
            String providerUserId = userManagementService.getProviderUserIdByEmail(reqUserDTO.getEmail());
            log.info("Found providerUserId for password change: {}", providerUserId);

            // Step 2: Ubah password di Keycloak
            keycloakAdminClientService.changeUserPassword(providerUserId, reqUserDTO.getPassword());
            log.info("Password changed in Keycloak for user: {}", reqUserDTO.getEmail());

            result.setStatus("success");
            result.setMessage("Password berhasil diubah");
        } catch (BusinessException e) {
            log.error("Failed to change password: {}", e.getErrorMessage());
            result.setStatus("failed");
            result.setMessage(e.getErrorMessage());
        }
        return result;
    }

    /**
     * Mengubah status aktif pengguna.
     * Orchestration:
     * 1. Update status di database
     * 2. Update status di Keycloak
     */
    public UserDTO setUserStatus(String email, boolean status) {
        UserDTO result = new UserDTO();
        result.setEmail(email);
        result.setActive(status);
        try {
            // Step 1: Dapatkan provider user ID
            String providerUserId = userManagementService.getProviderUserIdByEmail(email);
            log.info("Found providerUserId for status update: {}", providerUserId);

            // Step 2: Update status di database
            userManagementService.updateUserStatusInDb(email, status);
            log.info("User status updated in DB: {} = {}", email, status);

            // Step 3: Update status di Keycloak
            keycloakAdminClientService.updateUserStatus(providerUserId, status);
            log.info("User status updated in Keycloak: {} = {}", email, status);

            result.setStatus("success");
            result.setMessage("Status pengguna berhasil diubah");
        } catch (BusinessException e) {
            log.error("Failed to set user status: {}", e.getErrorMessage());
            result.setStatus("failed");
            result.setMessage(e.getErrorMessage());
        }
        return result;
    }

    /**
     * Mengubah status aktif backoffice user.
     * Langsung update di Keycloak menggunakan providerUserId.
     * Mendukung Sync-on-Login (user mungkin belum ada di DB local).
     */
    @CacheEvict(value = "backofficeUsers", cacheManager = "shortTtlCacheManager", allEntries = true)
    public UserDTO setBackofficeUserStatus(String providerUserId, boolean status) {
        UserDTO result = new UserDTO();
        result.setProviderUserId(providerUserId);
        result.setActive(status);
        try {
            // Update status di Keycloak langsung
            keycloakAdminClientService.updateUserStatus(providerUserId, status);
            log.info("Backoffice user status updated in Keycloak: {} = {}", providerUserId, status);

            result.setStatus("success");
            result.setMessage("Status pengguna berhasil diubah");
        } catch (Exception e) {
            log.error("Failed to set backoffice user status: {}", e.getMessage());
            result.setStatus("failed");
            result.setMessage("Gagal mengubah status: " + e.getMessage());
        }
        return result;
    }

    // ==================== User-Group Management ====================

    /**
     * Assign user ke group.
     *
     * @param userId  Provider user ID dari Keycloak
     * @param groupId Group ID
     */
    public Map<String, String> assignUserToGroup(String userId, String groupId) {
        keycloakAdminClientService.assignUserToGroup(userId, groupId);
        log.info("User '{}' assigned to group '{}'", userId, groupId);
        return Map.of(
                "status", "success",
                "message", "User assigned to group successfully");
    }

    /**
     * Remove user dari group.
     *
     * @param userId  Provider user ID dari Keycloak
     * @param groupId Group ID
     */
    public Map<String, String> removeUserFromGroup(String userId, String groupId) {
        keycloakAdminClientService.removeUserFromGroup(userId, groupId);
        log.info("User '{}' removed from group '{}'", userId, groupId);
        return Map.of(
                "status", "success",
                "message", "User removed from group successfully");
    }

    /**
     * Mendapatkan groups yang dimiliki user.
     *
     * @param userId Provider user ID dari Keycloak
     */
    public List<KeycloakGroupDTO> getUserGroups(String userId) {
        return keycloakAdminClientService.getUserGroups(userId);
    }

    /**
     * Mendapatkan members dari suatu group.
     *
     * @param groupId Group ID
     */
    public List<UserDTO> getGroupMembers(String groupId) {
        return getGroupMembers(groupId, false);
    }

    /**
     * Mendapatkan members dari suatu group.
     * 
     * @param recursive jika true, ambil users dari subgroup juga
     */
    public List<UserDTO> getGroupMembers(String groupId, boolean recursive) {
        return keycloakAdminClientService.getGroupMembers(groupId, recursive).stream()
                .map(member -> {
                    UserDTO dto = new UserDTO();
                    dto.setProviderUserId(member.providerUserId());
                    dto.setUsername(member.username());
                    dto.setFullname(member.fullname());
                    dto.setEmail(member.email());
                    dto.setActive(member.active());
                    return dto;
                })
                .toList();
    }

    /**
     * Mendapatkan groups dengan hierarchy (parent-child).
     */
    public List<KeycloakGroupDTO> getGroupsHierarchy() {
        return keycloakAdminClientService.getGroupsHierarchy();
    }

    /**
     * Mendapatkan subgroups dari parent group path.
     *
     * @param parentPath Path dari parent group (e.g., "/backoffice")
     */
    public List<KeycloakGroupDTO> getSubGroups(String parentPath) {
        return keycloakAdminClientService.getSubGroups(parentPath);
    }

    // ==================== Backoffice Users ====================

    /**
     * Get all users under /backoffice hierarchy with their groups populated.
     * This is a convenience method for the frontend - single API call.
     * 
     * ENHANCED: Fetches all realm users with their roles.
     * Supports filtering by composite role (matches users having any child role).
     *
     * @return List of UserDTO with roles populated
     */
    public List<UserDTO> getBackofficeUsers() {
        return getBackofficeUsers(null);
    }

    /**
     * Get backoffice users with optional filtering by composite role.
     * When roleFilter is provided, expands composite role to children
     * and returns users having ANY of those child roles.
     * 
     * OPTIMIZED: Uses batch fetching with parallel execution instead of N+1 loop.
     * CACHED: 30 seconds TTL via shortTtlCacheManager.
     *
     * @param roleFilter Optional composite role name to filter by
     * @return List of UserDTO with roles populated
     */
    @Cacheable(value = "backofficeUsers", cacheManager = "shortTtlCacheManager", unless = "#result.isEmpty()")
    public List<UserDTO> getBackofficeUsers(String roleFilter) {
        log.info("Fetching backoffice users (cache miss) with roleFilter: {}", roleFilter);

        // OPTIMIZED: Use batch method with parallel execution instead of N+1 loop
        List<UserDTO> users = keycloakAdminClientService.getUsersWithRolesBatch(100);

        // Apply filter if provided
        if (roleFilter != null && !roleFilter.isEmpty() && !"all".equalsIgnoreCase(roleFilter)) {
            // Get child role names from composite role (defensive copy — cached result may be unmodifiable)
            java.util.Set<String> childRoleNames = new java.util.HashSet<>(
                    keycloakAdminClientService.getCompositeRoleChildNames(roleFilter));
            // Also include the parent role itself
            childRoleNames.add(roleFilter);

            // Filter users who have ANY of the child roles
            users = users.stream()
                    .filter(user -> user.getRoleDetails() != null && user.getRoleDetails().stream()
                            .anyMatch(role -> childRoleNames.contains(role.getName())))
                    .toList();
        }

        return users;
    }

    /**
     * Get all roles for dropdown filtering.
     * Returns ALL realm roles (for granular filtering by any role).
     * Used for SSR initial data population.
     *
     * @return List of KeycloakGroupDTO (using same DTO for compatibility, populated
     *         from roles)
     */
    public List<KeycloakGroupDTO> getBackofficeSubGroups() {
        // Get ALL roles for dropdown (not just root roles)
        List<KeycloakRoleDTO> allRoles = keycloakAdminClientService.getRoles();

        return allRoles.stream()
                .map(role -> KeycloakGroupDTO.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .path("/" + role.getName())
                        .build())
                .toList();
    }

    /**
     * Get roles with hierarchy for dropdown display.
     * Composite roles will have their children populated.
     *
     * @return List of KeycloakRoleDTO with hierarchy
     */
    public List<KeycloakRoleDTO> getRolesForDropdown() {
        return keycloakAdminClientService.getRolesWithHierarchy();
    }
}
