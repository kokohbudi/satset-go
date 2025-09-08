package com.omnip.controllers;

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
}
