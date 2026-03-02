package com.omnip.catalog.adapter.in.web;

import com.omnip.catalog.domain.model.CategoryType;
import com.omnip.catalog.domain.model.DenomType;
import com.omnip.shared.constant.OmniConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequestMapping("/admin/catalog")
@PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
public class AdminCatalogPageController {

    @GetMapping
    public String catalogRoot() {
        return "redirect:/admin/catalog/categories";
    }

    @GetMapping("/categories")
    public String categoriesPage(Model model) {
        model.addAttribute("currentPage", "admin-catalog");
        model.addAttribute("breadcrumb", "Kategori Produk");
        model.addAttribute("categoryTypes", CategoryType.values());
        return "pages/admin/catalog/categories";
    }

    @GetMapping("/products")
    public String productsPage(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String categoryName,
            Model model) {
        model.addAttribute("currentPage", "admin-catalog");
        model.addAttribute("breadcrumb", "Produk");
        model.addAttribute("categoryId", categoryId != null ? categoryId : "");
        model.addAttribute("categoryName", categoryName != null ? categoryName : "");
        return "pages/admin/catalog/products";
    }

    @GetMapping("/products/{productId}/denoms")
    public String denomsPage(@PathVariable UUID productId, Model model) {
        model.addAttribute("currentPage", "admin-catalog");
        model.addAttribute("breadcrumb", "Denominasi");
        model.addAttribute("productId", productId);
        model.addAttribute("denomTypes", DenomType.values());
        return "pages/admin/catalog/denoms";
    }
}
