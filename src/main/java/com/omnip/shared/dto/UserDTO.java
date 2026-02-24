package com.omnip.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.omnip.onboarding.domain.model.Stores;
import com.omnip.identity.adapter.in.web.dto.KeycloakRoleDTO;
import com.omnip.identity.adapter.in.web.dto.KeycloakGroupDTO;
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
