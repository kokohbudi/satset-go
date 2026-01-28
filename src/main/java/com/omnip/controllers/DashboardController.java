package com.omnip.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class DashboardController {

    @GetMapping("/")
    public String landingPage(Authentication authentication) {
        // Check if user is truly authenticated (not anonymous)
        if (isAuthenticated(authentication)) {
            log.debug("User {} is authenticated, redirecting to dashboard", authentication.getName());
            return "redirect:/dashboard";
        }
        return "landing";
    }

    /**
     * Helper method to check if user is properly authenticated
     * (not anonymous and actually authenticated)
     */
    private boolean isAuthenticated(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }
        return authentication.isAuthenticated();
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Accessing dashboard");

        // Set page info for sidebar and header
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("breadcrumb", "Dashboard");

        return "pages/dashboard/index";
    }
}
