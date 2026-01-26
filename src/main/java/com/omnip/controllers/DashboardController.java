package com.omnip.controllers;

import com.omnip.entities.Menus;
import com.omnip.entities.Users;
import com.omnip.repositories.UsersRepository;
import com.omnip.services.UserRoleService;
import com.omnip.utils.MenuUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@Slf4j
public class DashboardController {

    private final UserRoleService userRoleService;
    private final UsersRepository usersRepository;
    private final MenuUtils menuUtils;

    public DashboardController(UserRoleService userRoleService,
            UsersRepository usersRepository,
            MenuUtils menuUtils) {
        this.userRoleService = userRoleService;
        this.usersRepository = usersRepository;
        this.menuUtils = menuUtils;
    }

    @GetMapping("/")
    public String landingPage(Authentication authentication) {
        // If user is authenticated, redirect to dashboard
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "pages/landingPage";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        log.info("Accessing dashboard");

        // Set page info for sidebar and header
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("breadcrumb", "Dashboard");

        try {
            // Get current user info
            Users currentUser = getCurrentUser(authentication);
            if (currentUser != null) {
                model.addAttribute("currentUser", currentUser);

                // Get user's accessible menus
                List<Menus> accessibleMenus = menuUtils.getAccessibleMenus(authentication);
                model.addAttribute("userMenus", accessibleMenus);

                // Get user access level for UI customization
                int accessLevel = menuUtils.getUserAccessLevel(authentication);
                String accessLevelName = menuUtils.getUserAccessLevelName(authentication);
                model.addAttribute("userAccessLevel", accessLevel);
                model.addAttribute("userAccessLevelName", accessLevelName);

                // Check specific permissions for conditional UI elements
                model.addAttribute("canManageRoles", menuUtils.hasMenuAccess(authentication, "ROLE_MANAGEMENT"));
                model.addAttribute("canManageUsers", menuUtils.hasMenuAccess(authentication, "USER_MANAGEMENT"));
                model.addAttribute("isAdmin", menuUtils.isAdmin(authentication));
                model.addAttribute("isOperator", menuUtils.isOperator(authentication));

                log.info("Dashboard loaded for user: {} with access level: {}",
                        currentUser.getEmail(), accessLevelName);
            }

        } catch (Exception e) {
            log.error("Error loading dashboard", e);
            model.addAttribute("error", "Terjadi kesalahan saat memuat dashboard");
        }

        return "pages/dashboard/index";
    }

    /**
     * Helper method untuk mendapatkan current user dari authentication.
     */
    private Users getCurrentUser(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String providerUserId = oidcUser.getSubject();
            return usersRepository.findByProviderUserId(providerUserId);
        }
        return null;
    }
}
