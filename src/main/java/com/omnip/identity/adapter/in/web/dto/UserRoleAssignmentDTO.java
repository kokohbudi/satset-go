package com.omnip.identity.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.omnip.shared.dto.BaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({
        "targetSource", "advisors", "targetClass", "frozen", "exposeProxy", "preFiltered",
        "targetObject", "proxyTargetClass", "advisorCount", "proxiedInterfaces"
})
@Data
public class UserRoleAssignmentDTO extends BaseDTO {
    private UUID userId;
    private String username;
    private String fullname;
    private String email;
    private List<UUID> roleIds;
    private List<RoleDTO> roles;
    private UUID assignedByUserId;
    private String assignedByUsername;
    private LocalDateTime assignedAt;
    private String notes;
}
