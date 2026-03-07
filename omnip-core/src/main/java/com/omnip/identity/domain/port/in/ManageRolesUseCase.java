package com.omnip.identity.domain.port.in;

import com.omnip.identity.domain.model.KeycloakRole;
import com.omnip.shared.exception.BusinessException;

import java.util.List;
import java.util.Map;

/**
 * Input port for managing roles and role-group assignments.
 */
public interface ManageRolesUseCase {

    List<KeycloakRole> getRoles();

    List<KeycloakRole> getRolesByScope(String scope);

    List<KeycloakRole> getRolesByGroup(String groupId) throws BusinessException;

    Map<String, String> assignRoleToGroup(String groupId, String roleName) throws BusinessException;

    Map<String, String> unassignRoleFromGroup(String groupId, String roleName) throws BusinessException;

    void assignRoleToUser(String userId, String roleName) throws BusinessException;

    void unassignRoleFromUser(String userId, String roleName) throws BusinessException;

    List<KeycloakRole> getRolesForDropdown();
}
