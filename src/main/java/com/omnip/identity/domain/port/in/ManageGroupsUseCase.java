package com.omnip.identity.domain.port.in;

import com.omnip.identity.adapter.in.web.dto.KeycloakGroupDTO;
import com.omnip.shared.dto.UserDTO;

import java.util.List;
import java.util.Map;

/**
 * Input port for managing groups and user-group membership.
 * TODO: Replace KeycloakGroupDTO/UserDTO with domain models when available.
 */
public interface ManageGroupsUseCase {

    List<KeycloakGroupDTO> getGroups();

    List<KeycloakGroupDTO> getGroupsHierarchy();

    List<KeycloakGroupDTO> getSubGroups(String parentPath);

    List<KeycloakGroupDTO> getBackofficeSubGroups();

    Map<String, String> assignUserToGroup(String userId, String groupId);

    Map<String, String> removeUserFromGroup(String userId, String groupId);

    List<KeycloakGroupDTO> getUserGroups(String userId);

    List<UserDTO> getGroupMembers(String groupId);

    List<UserDTO> getGroupMembers(String groupId, boolean recursive);
}
