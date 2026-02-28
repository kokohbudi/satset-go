package com.omnip.identity.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.omnip.shared.dto.BaseDTO;
import com.omnip.shared.dto.MenuDTO;
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
public class RoleDTO extends BaseDTO {
    private UUID id;
    private String roleCode;
    private String roleName;
    private String description;
    private List<MenuDTO> menus;
    private List<UUID> menuIds;
    private boolean active;
}
