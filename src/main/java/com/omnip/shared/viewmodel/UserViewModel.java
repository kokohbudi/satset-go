package com.omnip.shared.viewmodel;

import com.omnip.identity.domain.model.KeycloakRole;
import com.omnip.shared.dto.UserDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * ViewModel for displaying User data in Thymeleaf templates.
 * Contains pre-formatted fields to reduce logic in templates.
 * Implements "Fat Controller, Thin Template" pattern.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserViewModel extends UserDTO {

    private String initials;
    private String formattedDate;
    private String statusBadgeClass;
    private String statusDotClass;
    private String statusText;
    // Helper list of display role names
    private List<String> displayRoles;

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
        // createdAt logic handles if UserDTO eventually gets this field
        // this.setCreatedAt(user.getCreatedAt());

        // Compute View Fields
        this.initials = computeInitials(user.getFullname(), user.getUsername());

        // Mocking date for now as UserDTO doesn't seem to have it populated yet
        // In real impl, would be: this.formattedDate = user.getCreatedAt() != null ?
        // DATE_FORMATTER.format(user.getCreatedAt()) : "-";
        this.formattedDate = "-";

        this.statusText = user.isActive() ? "Aktif" : "Nonaktif";
        this.statusBadgeClass = user.isActive() ? "badge-success" : "badge-ghost";
        this.statusDotClass = user.isActive() ? "bg-success animate-pulse" : "bg-base-content/30";

        this.displayRoles = Optional.ofNullable(user.getRoleDetails())
                .orElse(Collections.emptyList())
                .stream()
                .map(KeycloakRole::getName) // Or getDisplay_name if available in attributes
                .toList();
    }

    private String computeInitials(String fullname, String username) {
        String source = (fullname != null && !fullname.isBlank()) ? fullname : username;
        if (source == null || source.isBlank())
            return "U";
        return source.length() >= 2 ? source.substring(0, 2).toUpperCase() : source.toUpperCase();
    }
}
