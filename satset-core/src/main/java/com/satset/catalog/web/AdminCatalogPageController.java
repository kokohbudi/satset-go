package com.satset.catalog.web;

import com.satset.catalog.dto.CategoryDTO;
import com.satset.catalog.dto.ProductDTO;
import com.satset.catalog.model.*;
import com.satset.catalog.service.CategoryDomainService;
import com.satset.catalog.service.ProductDomainService;
import com.satset.shared.constant.OmniConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/catalog")
@PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
public class AdminCatalogPageController {

    private final CategoryDomainService manageCategoriesUseCase;
    private final ProductDomainService manageProductsUseCase;

    public AdminCatalogPageController(CategoryDomainService manageCategoriesUseCase,
                                      ProductDomainService manageProductsUseCase) {
        this.manageCategoriesUseCase = manageCategoriesUseCase;
        this.manageProductsUseCase = manageProductsUseCase;
    }

    // "/categories" kept: the sidebar nav URL lives in the Keycloak view_catalog
    // role attribute (url=/admin/catalog/categories); serve the single page there too.
    @GetMapping({"", "/categories"})
    public String catalogRoot(Model model) {
        model.addAttribute("currentPage", "admin-catalog");
        model.addAttribute("breadcrumb", "Katalog");
        model.addAttribute("categoryTypes", CategoryType.values());
        model.addAttribute("denomTypes", DenomType.values());

        List<Category> allCategories = manageCategoriesUseCase.findAllForAdmin();
        List<CategoryDTO> categories = allCategories.stream()
                .map(CatalogDtoMapper::toCategoryDTO).toList();
        model.addAttribute("initialCategories", categories);

        List<ProductDTO> products = manageProductsUseCase.findAllForAdmin().stream()
                .map(CatalogDtoMapper::toProductDTO).toList();
        model.addAttribute("initialProducts", products);

        return "pages/admin/catalog/index";
    }
}
