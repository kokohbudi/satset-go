package com.satset.shared.viewmodel;

import com.satset.shared.dto.UserDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ViewModel for displaying User data in Thymeleaf templates.
 * Contains pre-formatted fields to reduce logic in templates.
 * Implements "Fat Controller, Thin Template" pattern.
 * 
 * Uses shared RoleInfo instead of domain KeycloakRole.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserViewModel extends UserDTO {

    private String initials;
    private String statusBadgeClass;
    private String statusDotClass;
    private String statusText;

    public UserViewModel(UserDTO user) {
        // Copy properties from UserDTO
        this.setEmail(user.getEmail());
        this.setUsername(user.getUsername());
        this.setFullname(user.getFullname());
        this.setRoles(user.getRoles());
        this.setRoleDetails(user.getRoleDetails());
        this.setProviderUserId(user.getProviderUserId());
        this.setActive(user.isActive());
        this.setGroups(user.getGroups());

        this.initials = computeInitials(user.getFullname(), user.getUsername());

        this.statusText = user.isActive() ? "Aktif" : "Nonaktif";
        this.statusBadgeClass = user.isActive() ? "badge-success" : "badge-ghost";
        this.statusDotClass = user.isActive() ? "bg-success animate-pulse" : "bg-base-content/30";
    }

    private String computeInitials(String fullname, String username) {
        String source = (fullname != null && !fullname.isBlank()) ? fullname : username;
        if (source == null || source.isBlank())
            return "U";
        return source.length() >= 2 ? source.substring(0, 2).toUpperCase() : source.toUpperCase();
    }
}
