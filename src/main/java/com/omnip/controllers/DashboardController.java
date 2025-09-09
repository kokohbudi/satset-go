package com.omnip.controllers;

import com.omnip.dtos.UserDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {



    @GetMapping("/")
    public String landingPage() {
        return "pages/landingPage";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "pages/dashboard";
    }

    @PreAuthorize("@userDTO.getRoles().contains('omnip-admin')")
    @GetMapping("/dashboard/test")
    public  String testDashboard()
    {
        return "pages/dashboard";
    }
}
