package com.omnip.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class DashboardController {

    @GetMapping("/")
    public String landingPage(Authentication authentication) {
        // If user is authenticated, redirect to dashboard
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "pages/landingPage";
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
