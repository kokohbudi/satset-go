package com.omnip.identity.domain.port.in;

import com.omnip.identity.adapter.in.web.dto.KeycloakRoleDTO;
import com.omnip.shared.exception.BusinessException;

import java.util.List;
import java.util.Map;

/**
 * Input port for managing roles and role-group assignments.
 * TODO: Replace KeycloakRoleDTO with domain model when available.
 */
public interface ManageRolesUseCase {

    List<KeycloakRoleDTO> getRoles();

    List<KeycloakRoleDTO> getRolesByScope(String scope);

    List<KeycloakRoleDTO> getRolesByGroup(String groupId) throws BusinessException;

    Map<String, String> assignRoleToGroup(String groupId, String roleName) throws BusinessException;

    Map<String, String> unassignRoleFromGroup(String groupId, String roleName) throws BusinessException;

    void assignRoleToUser(String userId, String roleName) throws BusinessException;

    void unassignRoleFromUser(String userId, String roleName) throws BusinessException;

    List<KeycloakRoleDTO> getRolesForDropdown();
}
