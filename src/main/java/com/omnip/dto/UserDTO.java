package com.omnip.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
@JsonIgnoreProperties({
        "targetSource", "advisors", "targetClass", "frozen", "exposeProxy", "preFiltered",
        "targetObject", "proxyTargetClass", "advisorCount", "proxiedInterfaces"
})
public class UserDTO {
    private String email;
    private String username;
    private String fullname;
    private String referalId;
    private List<String> roles;

    public String getEmail() {
        return this.email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getFullname() {
        return this.fullname;
    }
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
    public String getReferalId() {
        return this.referalId;
    }
    public void setReferalId(String referalId) {
        this.referalId = referalId;
    }
    public List<String> getRoles() {
        return this.roles;
    }
    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
