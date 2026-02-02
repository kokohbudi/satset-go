package com.omnip.controllers;

import com.omnip.services.IdentityManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for Admin pages (Thymeleaf frontend).
 * Handles the web UI routes for backoffice user management.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class UserPageController {

    private final IdentityManagementService identityManagementService;

    @GetMapping("/admin/user-management")
    @PreAuthorize("hasRole('view_users')")
    public String userManagementPage(Model model) {
        log.info("Accessing admin user management page");
        model.addAttribute("currentPage", "user-management");
        model.addAttribute("breadcrumb", "User Management");

        // SSR: Inject initial data for faster first paint
        model.addAttribute("initialUsers", identityManagementService.getBackofficeUsers());
        model.addAttribute("initialGroups", identityManagementService.getBackofficeSubGroups());
        // Roles with hierarchy for dropdown display
        model.addAttribute("rolesHierarchy", identityManagementService.getRolesForDropdown());

        return "pages/admin/user-management";
    }

    @GetMapping("/admin/role-attributes")
    public String roleAttributesPage(Model model) {
        log.info("Accessing admin role attributes page");
        model.addAttribute("currentPage", "role-attributes");
        model.addAttribute("breadcrumb", "Role Attributes");
        return "pages/admin/role-attributes";
    }
}
