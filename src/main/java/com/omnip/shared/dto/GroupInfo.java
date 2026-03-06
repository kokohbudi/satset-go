package com.omnip.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Shared DTO for group information.
 * Independent from domain layer - can be used in shared, adapter, and presentation layers.
 * 
 * This breaks the dependency from shared layer to identity.domain.model.KeycloakGroup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInfo {

    /**
     * Unique ID from Keycloak
     */
    private String id;

    /**
     * Group name (e.g., admin, operator)
     */
    private String name;

    /**
     * Full path of the group (e.g., /backoffice/admin)
     */
    private String path;

    /**
     * Parent group ID (null if top-level)
     */
    private String parentId;

    /**
     * Child groups (subgroups)
     */
    @Builder.Default
    private List<GroupInfo> subGroups = List.of();
}