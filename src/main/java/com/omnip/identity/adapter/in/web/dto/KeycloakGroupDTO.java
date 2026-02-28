package com.omnip.identity.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.GroupRepresentation;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO untuk representasi Group dari Keycloak.
 * Mendukung hierarchical groups dengan parent/child relationship.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakGroupDTO {

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
    private List<KeycloakGroupDTO> subGroups = new ArrayList<>();

    /**
     * Factory method untuk membuat KeycloakGroupDTO dari GroupRepresentation
     * Keycloak (tanpa subgroups - flat mode)
     *
     * @param groupRep GroupRepresentation dari Keycloak
     * @return KeycloakGroupDTO
     */
    public static KeycloakGroupDTO fromGroupRepresentation(GroupRepresentation groupRep) {
        return KeycloakGroupDTO.builder()
                .id(groupRep.getId())
                .name(groupRep.getName())
                .path(groupRep.getPath())
                .build();
    }

    /**
     * Factory method untuk membuat KeycloakGroupDTO dengan hierarchy (recursive)
     *
     * @param groupRep GroupRepresentation dari Keycloak
     * @param parentId ID dari parent group (null untuk top-level)
     * @return KeycloakGroupDTO dengan subGroups populated
     */
    public static KeycloakGroupDTO fromGroupRepresentationWithHierarchy(GroupRepresentation groupRep, String parentId) {
        KeycloakGroupDTO dto = KeycloakGroupDTO.builder()
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
