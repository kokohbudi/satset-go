package com.omnip.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.omnip.identity.domain.model.KeycloakRole;
import com.omnip.identity.domain.model.KeycloakGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

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
    private List<KeycloakRole> roleDetails;
    private String providerUserId;
    private String password;
    private UUID storeId;
    private boolean active;
    private List<KeycloakGroup> groups;

}
