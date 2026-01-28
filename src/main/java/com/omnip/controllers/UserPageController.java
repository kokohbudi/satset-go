package com.omnip.controllers;

import com.omnip.services.IdentityManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for User Management pages (Thymeleaf frontend).
 * Handles the web UI routes for user CRUD operations.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class UserPageController {

    private final IdentityManagementService identityManagementService;

    @GetMapping("/admin/users")
    public String usersPage(Model model) {
        log.info("Accessing users list page");
        model.addAttribute("currentPage", "users");
        model.addAttribute("breadcrumb", "Daftar User");
        // TODO: Fetch users from service and add to model
        // model.addAttribute("users", userService.getAllUsers());
        return "pages/users/index";
    }

    @GetMapping("/admin/groups")
    public String groupsPage(Model model) {
        log.info("Accessing groups management page");
        model.addAttribute("currentPage", "groups");
        model.addAttribute("breadcrumb", "Group Management");
        // TODO: Fetch groups from service and add to model
        // model.addAttribute("groups", groupService.getAllGroups());
        return "pages/groups/index";
    }

    @GetMapping("/admin/user-groups")
    public String userGroupsPage(Model model) {
        log.info("Accessing user-groups assignment page");
        model.addAttribute("currentPage", "user-groups");
        model.addAttribute("breadcrumb", "Assign User ke Group");
        // TODO: Fetch available users and groups from service
        // model.addAttribute("availableUsers", userService.getAvailableUsers());
        // model.addAttribute("groups", groupService.getAllGroups());
        return "pages/user-groups/index";
    }

    // ==================== Admin Pages (New) ====================

    @GetMapping("/admin/user-management")
    @PreAuthorize("@authz.hasGroupPrefix('/backoffice/') and hasRole('view_users')")
    public String userManagementPage(Model model) {
        log.info("Accessing admin user management page");
        model.addAttribute("currentPage", "user-management");
        model.addAttribute("breadcrumb", "User Management");

        // SSR: Inject initial data for faster first paint
        model.addAttribute("initialUsers", identityManagementService.getBackofficeUsers());
        model.addAttribute("initialGroups", identityManagementService.getBackofficeSubGroups());

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
