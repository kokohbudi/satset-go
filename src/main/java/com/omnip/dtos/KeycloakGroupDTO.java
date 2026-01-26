package com.omnip.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.GroupRepresentation;

/**
 * DTO untuk representasi Group dari Keycloak.
 * Hanya berisi id dan nama group.
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
     * Nama group (misal: bo-admin, bo-operator)
     */
    private String name;

    /**
     * Factory method untuk membuat KeycloakGroupDTO dari GroupRepresentation
     * Keycloak
     *
     * @param groupRep GroupRepresentation dari Keycloak
     * @return KeycloakGroupDTO
     */
    public static KeycloakGroupDTO fromGroupRepresentation(GroupRepresentation groupRep) {
        return KeycloakGroupDTO.builder()
                .id(groupRep.getId())
                .name(groupRep.getName())
                .build();
    }
}
