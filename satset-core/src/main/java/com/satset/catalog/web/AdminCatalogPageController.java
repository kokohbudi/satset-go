package com.satset.catalog.web;

import com.satset.catalog.model.*;
import com.satset.catalog.service.category.CategoryDomainService;
import com.satset.catalog.service.denom.DenomDomainService;
import com.satset.catalog.service.product.ProductDomainService;
import com.satset.shared.constant.SatsetConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/catalog")
@PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_CATALOG + "')")
public class AdminCatalogPageController {

    private final CategoryDomainService manageCategoriesUseCase;
    private final ProductDomainService manageProductsUseCase;
    private final DenomDomainService manageDenomsUseCase;

    public AdminCatalogPageController(CategoryDomainService manageCategoriesUseCase,
                                      ProductDomainService manageProductsUseCase,
                                      DenomDomainService manageDenomsUseCase) {
        this.manageCategoriesUseCase = manageCategoriesUseCase;
        this.manageProductsUseCase = manageProductsUseCase;
        this.manageDenomsUseCase = manageDenomsUseCase;
    }

    // "/categories" kept: the sidebar nav URL lives in the Keycloak view_catalog
    // role attribute (url=/admin/catalog/categories); serve the single page there too.
    @GetMapping({"", "/categories"})
    public String catalogRoot(Model model) {
        model.addAttribute("currentPage", "admin-catalog");
        model.addAttribute("breadcrumb", "Katalog");
        model.addAttribute("categoryTypes", CategoryType.values());
        model.addAttribute("denomTypes", DenomType.values());

        model.addAttribute("initialCategories", manageCategoriesUseCase.findAllForAdmin());
        model.addAttribute("initialProducts", manageProductsUseCase.findAllForAdmin());
        model.addAttribute("initialDenoms", manageDenomsUseCase.findAllForAdmin());

        return "pages/admin/catalog/index";
    }
}
