package com.omnip.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.omnip.entities.Store;
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
    private String providerUserId;
    private String password;
    private Store store;
    private boolean active;


}
