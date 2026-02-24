package com.omnip.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class AdminResellerPageController {

    @GetMapping("/admin/resellers")
    public String resellerFormPage(Model model) {
        log.info("Accessing admin reseller form page");
        model.addAttribute("currentPage", "resellers");
        model.addAttribute("breadcrumb", "Tambah Reseller");
        return "pages/admin/reseller-form";
    }
}
