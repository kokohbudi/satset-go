package com.omnip.identity.adapter.in.web;

import com.omnip.identity.domain.port.in.ManageBackofficeUsersUseCase;
import com.omnip.identity.domain.port.in.ManageGroupsUseCase;
import com.omnip.identity.domain.port.in.ManageRolesUseCase;
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

    private final ManageBackofficeUsersUseCase manageBackofficeUsersUseCase;
    private final ManageGroupsUseCase manageGroupsUseCase;
    private final ManageRolesUseCase manageRolesUseCase;

    @GetMapping("/admin/user-management")
    @PreAuthorize("hasRole('REALM_view_users')")
    public String userManagementPage(Model model) {
        log.info("Accessing admin user management page");
        model.addAttribute("currentPage", "user-management");
        model.addAttribute("breadcrumb", "User Management");

        // SSR: Inject initial data for faster first paint
        model.addAttribute("initialUsers", manageBackofficeUsersUseCase.getBackofficeUsers().stream()
                .map(com.omnip.shared.viewmodel.UserViewModel::new)
                .toList());
        model.addAttribute("initialGroups", manageGroupsUseCase.getBackofficeSubGroups());
        model.addAttribute("rolesHierarchy", manageRolesUseCase.getRolesForDropdown());

        return "pages/admin/user-management";
    }

}
