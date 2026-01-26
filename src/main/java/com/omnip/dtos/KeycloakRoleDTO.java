package com.omnip.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.Map;

/**
 * DTO untuk representasi Role dari Keycloak.
 * Digunakan untuk response API yang mengembalikan roles dari Keycloak Admin
 * Client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakRoleDTO {

    /**
     * ID unik dari role di Keycloak
     */
    private String id;

    /**
     * Nama role (misal: manage_users, view_users)
     */
    private String name;

    /**
     * Deskripsi role
     */
    private String description;

    /**
     * True jika ini adalah client role, false jika realm role
     */
    private Boolean clientRole;

    /**
     * True jika role ini composite (terdiri dari role lain)
     */
    private Boolean composite;

    /**
     * ID dari container (client ID untuk client role, realm ID untuk realm role)
     */
    private String containerId;

    /**
     * Atribut tambahan pada role
     */
    private Map<String, String> attributes;

    /**
     * Factory method untuk membuat KeycloakRoleDTO dari RoleRepresentation Keycloak
     *
     * @param roleRep RoleRepresentation dari Keycloak
     * @return KeycloakRoleDTO
     */
    public static KeycloakRoleDTO fromRoleRepresentation(RoleRepresentation roleRep) {
        return KeycloakRoleDTO.builder()
                .id(roleRep.getId())
                .name(roleRep.getName())
                .description(roleRep.getDescription())
                .clientRole(roleRep.getClientRole())
                .composite(roleRep.isComposite())
                .containerId(roleRep.getContainerId())
                .attributes(roleRep.getAttributes() != null ? roleRep.getAttributes().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue() != null && !e.getValue().isEmpty() ? e.getValue().get(0) : null))
                        : null)
                .build();
    }
}
