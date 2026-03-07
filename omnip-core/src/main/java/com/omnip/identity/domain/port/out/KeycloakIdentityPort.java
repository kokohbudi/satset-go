package com.omnip.identity.domain.port.out;

import com.omnip.identity.domain.model.KeycloakGroup;
import com.omnip.identity.domain.model.KeycloakRole;
import com.omnip.identity.domain.model.GroupMemberInfo;
import com.omnip.shared.dto.GroupInfo;
import com.omnip.shared.dto.RoleInfo;
import com.omnip.shared.dto.UserDTO;
import com.omnip.shared.exception.BusinessException;

import java.util.List;
import java.util.Set;

/**
 * Output port for identity provider operations (Keycloak).
 * Abstracts all Keycloak Admin Client interactions.
 * 
 * Note: Methods returning RoleInfo/GroupInfo are provided for shared layer
 * consumption to avoid coupling shared layer to domain models.
 */
public interface KeycloakIdentityPort {

    // ==================== Groups ====================

    List<KeycloakGroup> getGroups();

    List<KeycloakGroup> getGroupsHierarchy();

    List<KeycloakGroup> getSubGroups(String parentPath);

    void assignUserToGroup(String userId, String groupId);

    void removeUserFromGroup(String userId, String groupId);

    List<KeycloakGroup> getUserGroups(String userId);

    List<GroupMemberInfo> getGroupMembers(String groupId, boolean recursive);

    // ==================== Roles ====================

    List<KeycloakRole> getRoles();

    List<KeycloakRole> getRolesByScope(String scope);

    List<KeycloakRole> getRolesByGroup(String groupId) throws BusinessException;

    void assignRoleToGroup(String groupId, String roleName) throws BusinessException;

    void unassignRoleFromGroup(String groupId, String roleName) throws BusinessException;

    void assignRoleToUser(String userId, String roleName) throws BusinessException;

    void unassignRoleFromUser(String userId, String roleName) throws BusinessException;

    KeycloakRole getCachedRoleWithAttributes(String roleName);

    List<KeycloakRole> getRolesWithHierarchy();

    Set<String> getCompositeRoleChildNames(String compositeRoleName);

    // ==================== Users ====================

    List<UserDTO> getAllKeycloakUsers(int maxResults);

    String createBackofficeUser(String username, String fullname, String email, String password, String role)
            throws BusinessException;

    void changeUserPassword(String providerUserId, String newPassword);

    void updateUserStatus(String providerUserId, boolean enabled);

    List<UserDTO> getUsersWithRolesBatch(int maxResults);

    boolean userExistsByEmail(String email);

    // ==================== Organizations ====================

    String createOrganization(String orgName) throws BusinessException;

    void addMemberToOrganization(String orgId, String userId) throws BusinessException;

    String createResellerUser(String username, String fullname, String email) throws BusinessException;

    void assignClientRoleToUser(String userId, String roleName) throws BusinessException;

    // ==================== Menu/UI ====================

    List<KeycloakRole> getMenuRoles(String userId) throws BusinessException;

    // ==================== Shared DTO Methods ====================
    // These methods return shared DTOs for use by shared layer components
    // to avoid coupling shared layer to domain models.

    /**
     * Get menu roles for a user as shared DTOs.
     * Use this method from shared layer components.
     */
    List<RoleInfo> getMenuRoleInfos(String userId) throws BusinessException;

    /**
     * Get user groups as shared DTOs.
     * Use this method from shared layer components.
     */
    List<GroupInfo> getUserGroupInfos(String userId) throws BusinessException;
}
