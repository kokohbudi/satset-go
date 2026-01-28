package com.omnip.services;

import com.omnip.dtos.KeycloakGroupDTO;
import com.omnip.dtos.KeycloakRoleDTO;
import com.omnip.dtos.UserDTO;
import com.omnip.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
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
public class IdentityManagementService {

    private final KeycloakAdminClientService keycloakAdminClientService;
    private final UserManagementService userManagementService;

    public IdentityManagementService(
            KeycloakAdminClientService keycloakAdminClientService,
            UserManagementService userManagementService) {
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

    // ==================== User Management ====================

    /**
     * Membuat akun pengguna baru.
     * Orchestration:
     * 1. Buat user di Keycloak (via KeycloakAdminClientService)
     * 2. Simpan user ke database (via UserManagementService)
     */
    public UserDTO createUser(UserDTO reqUserDTO) {
        try {
            // Step 1: Buat user di Keycloak
            String providerUserId = keycloakAdminClientService.createUser(
                    reqUserDTO.getUsername(),
                    reqUserDTO.getFullname(),
                    reqUserDTO.getEmail(),
                    reqUserDTO.getPassword(),
                    reqUserDTO.getRoles().getFirst());
            log.info("User created in Keycloak with providerUserId: {}", providerUserId);

            // Step 2: Simpan user ke database
            UserDTO result = userManagementService.saveUserToDb(reqUserDTO, providerUserId);
            log.info("User saved to database: {}", reqUserDTO.getEmail());

            return result;
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
                .map(userRep -> {
                    UserDTO dto = new UserDTO();
                    dto.setProviderUserId(userRep.getId());
                    dto.setUsername(userRep.getUsername());
                    dto.setFullname(userRep.getFirstName() + " " +
                            (userRep.getLastName() != null ? userRep.getLastName() : ""));
                    dto.setEmail(userRep.getEmail());
                    dto.setActive(userRep.isEnabled());
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

    // ==================== Role Attributes ====================

    /**
     * Update attributes dari role.
     *
     * @param roleName   Nama role
     * @param attributes Map of attribute key to list of values
     */
    public Map<String, String> updateRoleAttributes(String roleName, Map<String, List<String>> attributes)
            throws BusinessException {
        keycloakAdminClientService.updateRoleAttributes(roleName, attributes);
        log.info("Role '{}' attributes updated", roleName);
        return Map.of(
                "status", "success",
                "message", "Role attributes updated successfully");
    }

    /**
     * Mendapatkan role dengan attributes (full detail).
     *
     * @param roleName Nama role
     */
    public KeycloakRoleDTO getRoleWithAttributes(String roleName) {
        return keycloakAdminClientService.getCachedRoleWithAttributes(roleName);
    }

    // ==================== Backoffice Users ====================

    /**
     * Get all users under /backoffice hierarchy with their groups populated.
     * This is a convenience method for the frontend - single API call.
     *
     * @return List of UserDTO with groups field populated
     */
    public List<UserDTO> getBackofficeUsers() {
        // 1. Find backoffice group
        List<KeycloakGroupDTO> allGroups = getGroupsHierarchy();
        KeycloakGroupDTO backofficeGroup = allGroups.stream()
                .filter(g -> "/backoffice".equals(g.getPath()))
                .findFirst()
                .orElse(null);

        if (backofficeGroup == null) {
            return List.of();
        }

        // 2. Get all members recursively
        List<UserDTO> users = getGroupMembers(backofficeGroup.getId(), true);

        // 3. Deduplicate by providerUserId (user can be in multiple groups)
        java.util.Map<String, UserDTO> uniqueUsers = new java.util.LinkedHashMap<>();
        for (UserDTO user : users) {
            uniqueUsers.putIfAbsent(user.getProviderUserId(), user);
        }

        // 4. Populate groups for each unique user
        List<UserDTO> result = new java.util.ArrayList<>(uniqueUsers.values());
        for (UserDTO user : result) {
            user.setGroups(getUserGroups(user.getProviderUserId()));
        }

        return result;
    }

    /**
     * Get subgroups under /backoffice for dropdown filtering.
     * Used for SSR initial data population.
     *
     * @return List of KeycloakGroupDTO (flat list of backoffice subgroups)
     */
    public List<KeycloakGroupDTO> getBackofficeSubGroups() {
        List<KeycloakGroupDTO> allGroups = getGroupsHierarchy();
        return allGroups.stream()
                .filter(g -> "/backoffice".equals(g.getPath()))
                .findFirst()
                .map(KeycloakGroupDTO::getSubGroups)
                .orElse(List.of());
    }
}
