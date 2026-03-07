package com.satset.onboarding.adapter.in.web;

import com.satset.shared.constant.OmniConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class AdminResellerPageController {

    @GetMapping("/admin/resellers")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_RESELLERS + "')")
    public String resellerFormPage(Model model) {
        log.info("Accessing admin reseller form page");
        model.addAttribute("currentPage", "resellers");
        model.addAttribute("breadcrumb", "Tambah Reseller");
        return "pages/admin/reseller-form";
    }
}
