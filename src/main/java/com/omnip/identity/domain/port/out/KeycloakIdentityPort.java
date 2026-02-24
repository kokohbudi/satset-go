package com.omnip.identity.domain.port.out;

import com.omnip.identity.adapter.in.web.dto.KeycloakGroupDTO;
import com.omnip.identity.adapter.in.web.dto.KeycloakRoleDTO;
import com.omnip.shared.dto.UserDTO;
import com.omnip.shared.exception.BusinessException;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Output port for identity provider operations (Keycloak).
 * Abstracts all Keycloak Admin Client interactions.
 * TODO: Replace adapter DTOs with domain models when available.
 */
public interface KeycloakIdentityPort {

    // ==================== Groups ====================

    List<KeycloakGroupDTO> getGroups();

    List<KeycloakGroupDTO> getGroupsHierarchy();

    List<KeycloakGroupDTO> getSubGroups(String parentPath);

    void assignUserToGroup(String userId, String groupId);

    void removeUserFromGroup(String userId, String groupId);

    List<KeycloakGroupDTO> getUserGroups(String userId);

    List<UserRepresentation> getGroupMembers(String groupId, boolean recursive);

    // ==================== Roles ====================

    List<KeycloakRoleDTO> getRoles();

    List<KeycloakRoleDTO> getRolesByScope(String scope);

    List<KeycloakRoleDTO> getRolesByGroup(String groupId) throws BusinessException;

    void assignRoleToGroup(String groupId, String roleName) throws BusinessException;

    void unassignRoleFromGroup(String groupId, String roleName) throws BusinessException;

    void assignRoleToUser(String userId, String roleName) throws BusinessException;

    void unassignRoleFromUser(String userId, String roleName) throws BusinessException;

    void updateRoleAttributes(String roleName, Map<String, List<String>> attributes) throws BusinessException;

    KeycloakRoleDTO getCachedRoleWithAttributes(String roleName);

    List<KeycloakRoleDTO> getRolesWithHierarchy();

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
}
