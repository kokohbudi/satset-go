package com.omnip.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PurchasePageController {

    @GetMapping("/purchase")
    public String purchasePage(Model model) {
        model.addAttribute("currentPage", "purchase");
        model.addAttribute("breadcrumb", "Beli Pulsa");
        return "pages/purchase/index";
    }
}
