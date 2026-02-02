package com.omnip.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.omnip.entities.Stores;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({
        "targetSource", "advisors", "targetClass", "frozen", "exposeProxy", "preFiltered",
        "targetObject", "proxyTargetClass", "advisorCount", "proxiedInterfaces"
})
@Data
public class UserDTO extends BaseDTO {
    private String email;
    private String username;
    private String fullname;
    private String referalId;
    private List<String> roles;
    /**
     * Full role details with attributes for UI display.
     * This is populated when enriching user data.
     */
    private List<KeycloakRoleDTO> roleDetails;
    private String providerUserId;
    private String password;
    private Stores stores;
    private boolean active;
    private List<KeycloakGroupDTO> groups;

}
