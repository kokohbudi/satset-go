package com.omnip.identity.adapter.in.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class UserProfileController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showProfilePage(@AuthenticationPrincipal Jwt jwt, Model model) {
        model.addAttribute("name", jwt.getClaimAsString("name"));
        model.addAttribute("email", jwt.getClaimAsString("email"));

        // Extract roles from realm_access if needed, or just let Thymeleaf handle it
        return "pages/identity/profile";
    }
}
