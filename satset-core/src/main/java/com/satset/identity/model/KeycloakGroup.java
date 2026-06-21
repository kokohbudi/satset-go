package com.satset.identity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.GroupRepresentation;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model untuk representasi Group dari Keycloak.
 * Mendukung hierarchical groups dengan parent/child relationship.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakGroup {

    /**
     * ID unik dari group di Keycloak
     */
    private String id;

    /**
     * Nama group (misal: admin, operator)
     */
    private String name;

    /**
     * Full path dari group (misal: /backoffice/admin)
     */
    private String path;

    /**
     * ID dari parent group (null jika top-level)
     */
    private String parentId;

    /**
     * Child groups (subgroups)
     */
    @Builder.Default
    private List<KeycloakGroup> subGroups = new ArrayList<>();

    /**
     * Factory method untuk membuat KeycloakGroup dari GroupRepresentation
     * Keycloak (tanpa subgroups - flat mode)
     *
     * @param groupRep GroupRepresentation dari Keycloak
     * @return KeycloakGroup
     */
    public static KeycloakGroup fromGroupRepresentation(GroupRepresentation groupRep) {
        return KeycloakGroup.builder()
                .id(groupRep.getId())
                .name(groupRep.getName())
                .path(groupRep.getPath())
                .build();
    }
}