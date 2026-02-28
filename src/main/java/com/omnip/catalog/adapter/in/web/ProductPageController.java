package com.omnip.catalog.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductPageController {

    @GetMapping("/admin/products")
    public String productsPage(Model model) {
        model.addAttribute("currentPage", "products");
        model.addAttribute("breadcrumb", "Katalog Produk");
        return "pages/products/index";
    }
}
