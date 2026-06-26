package com.satset.shared.web;

import com.satset.shared.constant.OmniConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PurchasePageController {

    @GetMapping("/purchase")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_PURCHASE + "')")
    public String purchasePage(Model model) {
        model.addAttribute("currentPage", "purchase");
        model.addAttribute("breadcrumb", "Beli Pulsa");
        return "pages/purchase/index";
    }
}
