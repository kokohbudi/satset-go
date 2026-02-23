package com.omnip.controllers;

import com.omnip.services.KeycloakAdminClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
public class AdminResellerPageController {

    private final KeycloakAdminClientService keycloakAdminClientService;

    @GetMapping("/admin/resellers")
    public String resellerFormPage(Model model) {
        log.info("Accessing admin reseller form page");
        model.addAttribute("currentPage", "resellers");
        model.addAttribute("breadcrumb", "Tambah Reseller");
        model.addAttribute("availableRoles", keycloakAdminClientService.getRoles());
        return "pages/admin/reseller-form";
    }
}
