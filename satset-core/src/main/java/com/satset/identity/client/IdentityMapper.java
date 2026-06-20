package com.satset.identity.client;

import com.satset.identity.model.KeycloakGroup;
import com.satset.identity.model.KeycloakRole;
import com.satset.shared.dto.GroupInfo;
import com.satset.shared.dto.RoleInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for converting domain models to shared DTOs.
 * This mapper lives in the adapter layer and bridges the gap between:
 * - identity.domain.model (KeycloakRole, KeycloakGroup)
 * - shared.dto (RoleInfo, GroupInfo)
 * 
 * This allows the shared layer to remain independent from domain models.
 */
@Component
public class IdentityMapper {

    /**
     * Convert KeycloakRole domain model to RoleInfo shared DTO.
     */
    public RoleInfo toRoleInfo(KeycloakRole role) {
        if (role == null) {
            return null;
        }
        
        return RoleInfo.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .clientRole(role.getClientRole())
                .composite(role.getComposite())
                .containerId(role.getContainerId())
                .attributes(role.getAttributes())
                .children(toRoleInfoList(role.getChildren()))
                .build();
    }

    /**
     * Convert list of KeycloakRole to list of RoleInfo.
     */
    public List<RoleInfo> toRoleInfoList(List<KeycloakRole> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(this::toRoleInfo)
                .toList();
    }

    /**
     * Convert KeycloakGroup domain model to GroupInfo shared DTO.
     */
    public GroupInfo toGroupInfo(KeycloakGroup group) {
        if (group == null) {
            return null;
        }
        
        return GroupInfo.builder()
                .id(group.getId())
                .name(group.getName())
                .path(group.getPath())
                .parentId(group.getParentId())
                .subGroups(toGroupInfoList(group.getSubGroups()))
                .build();
    }

    /**
     * Convert list of KeycloakGroup to list of GroupInfo.
     */
    public List<GroupInfo> toGroupInfoList(List<KeycloakGroup> groups) {
        if (groups == null) {
            return List.of();
        }
        return groups.stream()
                .map(this::toGroupInfo)
                .toList();
    }
}