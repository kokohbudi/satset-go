package com.satset.identity.domain.port.in;

import com.satset.identity.domain.model.KeycloakGroup;
import com.satset.shared.dto.UserDTO;

import java.util.List;
import java.util.Map;

/**
 * Input port for managing groups and user-group membership.
 */
public interface ManageGroupsUseCase {

    List<KeycloakGroup> getGroups();

    List<KeycloakGroup> getGroupsHierarchy();

    List<KeycloakGroup> getSubGroups(String parentPath);

    List<KeycloakGroup> getBackofficeSubGroups();

    Map<String, String> assignUserToGroup(String userId, String groupId);

    Map<String, String> removeUserFromGroup(String userId, String groupId);

    List<KeycloakGroup> getUserGroups(String userId);

    List<UserDTO> getGroupMembers(String groupId);

    List<UserDTO> getGroupMembers(String groupId, boolean recursive);
}
