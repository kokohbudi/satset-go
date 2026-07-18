package com.satset.identity.web;

import com.satset.identity.service.provisioning.IdentityDomainService;
import com.satset.shared.constant.SatsetConstants;
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

    private final IdentityDomainService identityService;

    @GetMapping("/admin/user-management")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_USERS + "')")
    public String userManagementPage(Model model) {
        log.info("Accessing admin user management page");
        model.addAttribute("currentPage", "user-management");
        model.addAttribute("breadcrumb", "User Management");

        // SSR: Inject initial data for faster first paint
        model.addAttribute("initialUsers", identityService.getBackofficeUsers().stream()
                .map(com.satset.shared.viewmodel.UserViewModel::new)
                .toList());
        model.addAttribute("initialGroups", identityService.getBackofficeSubGroups());
        model.addAttribute("rolesHierarchy", identityService.getRolesForDropdown());

        return "pages/admin/user-management";
    }

}
