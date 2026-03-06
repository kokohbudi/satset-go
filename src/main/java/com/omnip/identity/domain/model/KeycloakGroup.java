package com.omnip.identity.domain.model;

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
 * 
 * Dipindahkan dari adapter/in/web/dto untuk memenuhi hexagonal architecture:
 * Domain layer tidak boleh bergantung ke adapter layer.
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

    /**
     * Factory method untuk membuat KeycloakGroup dengan hierarchy (recursive)
     *
     * @param groupRep GroupRepresentation dari Keycloak
     * @param parentId ID dari parent group (null untuk top-level)
     * @return KeycloakGroup dengan subGroups populated
     */
    public static KeycloakGroup fromGroupRepresentationWithHierarchy(GroupRepresentation groupRep, String parentId) {
        KeycloakGroup dto = KeycloakGroup.builder()
                .id(groupRep.getId())
                .name(groupRep.getName())
                .path(groupRep.getPath())
                .parentId(parentId)
                .subGroups(new ArrayList<>())
                .build();

        // Recursively process subgroups
        if (groupRep.getSubGroups() != null && !groupRep.getSubGroups().isEmpty()) {
            for (GroupRepresentation subGroup : groupRep.getSubGroups()) {
                dto.getSubGroups().add(fromGroupRepresentationWithHierarchy(subGroup, groupRep.getId()));
            }
        }

        return dto;
    }
}