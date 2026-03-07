package com.omnip.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

/**
 * Shared DTO for user session data.
 * Independent from domain layer - uses RoleInfo and GroupInfo instead of domain models.
 * 
 * This breaks the dependency from shared layer to identity.domain.model package.
 */
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
     * Uses shared RoleInfo instead of domain KeycloakRole.
     */
    private List<RoleInfo> roleDetails;
    private String providerUserId;
    private String password;
    private UUID storeId;
    private boolean active;
    /**
     * Groups the user belongs to.
     * Uses shared GroupInfo instead of domain KeycloakGroup.
     */
    private List<GroupInfo> groups;
}
