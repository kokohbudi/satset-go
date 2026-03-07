package com.satset.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Shared DTO for role information.
 * Independent from domain layer - can be used in shared, adapter, and presentation layers.
 * 
 * This breaks the dependency from shared layer to identity.domain.model.KeycloakRole.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleInfo {

    /**
     * Unique ID from Keycloak
     */
    private String id;

    /**
     * Role name (e.g., manage_users, view_users)
     */
    private String name;

    /**
     * Role description
     */
    private String description;

    /**
     * True if client role, false if realm role
     */
    private Boolean clientRole;

    /**
     * True if composite role (contains other roles)
     */
    private Boolean composite;

    /**
     * Container ID (client ID for client role, realm ID for realm role)
     */
    private String containerId;

    /**
     * Additional attributes on the role
     */
    private Map<String, String> attributes;

    /**
     * Child roles (if composite)
     */
    @Builder.Default
    private List<RoleInfo> children = List.of();
}