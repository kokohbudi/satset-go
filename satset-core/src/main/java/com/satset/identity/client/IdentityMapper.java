package com.satset.identity.client;

import com.satset.identity.model.KeycloakRole;
import com.satset.shared.dto.RoleInfo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps identity models (KeycloakRole) to shared DTOs (RoleInfo).
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
}