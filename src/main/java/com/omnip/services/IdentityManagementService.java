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
     * Mendapatkan semua client roles dari Keycloak.
     */
    public List<KeycloakRoleDTO> getRoles() {
        return keycloakAdminClientService.getRoles();
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
}
