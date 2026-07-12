package com.satset.shared.viewmodel;

import com.satset.shared.dto.UserDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

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
        // Copy all matching properties from UserDTO; computed fields set below.
        BeanUtils.copyProperties(user, this);

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
