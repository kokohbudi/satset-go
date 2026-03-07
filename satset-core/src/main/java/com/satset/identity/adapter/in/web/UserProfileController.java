package com.satset.identity.adapter.in.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class UserProfileController {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String showProfilePage(@AuthenticationPrincipal OidcUser oidcUser, Model model) {
        // Fallback to "name" claim if full name is not available
        String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : oidcUser.getClaimAsString("name");
        model.addAttribute("name", name);
        model.addAttribute("email", oidcUser.getEmail());

        return "pages/identity/profile";
    }
}
