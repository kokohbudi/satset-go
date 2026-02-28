package com.omnip.shared.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class MenuDTO extends BaseDTO {
    private UUID id;
    private String menuCode;
    private String menuName;
    private String description;
    private String url;
    private String icon;
    private Integer sortOrder;
    private UUID parentMenuId;
    private String parentMenuName;
    private List<MenuDTO> subMenus;
    private boolean active;
}
