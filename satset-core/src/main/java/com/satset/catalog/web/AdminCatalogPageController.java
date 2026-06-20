package com.satset.catalog.web;

import com.satset.catalog.dto.CategoryDTO;
import com.satset.catalog.dto.ProductDTO;
import com.satset.catalog.dto.ProductDenomDTO;
import com.satset.catalog.model.*;
import com.satset.catalog.service.CategoryDomainService;
import com.satset.catalog.service.DenomDomainService;
import com.satset.catalog.service.ProductDomainService;
import com.satset.shared.constant.OmniConstants;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/catalog")
@PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
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

    @GetMapping
    public String catalogRoot() {
        return "redirect:/admin/catalog/categories";
    }

    @GetMapping("/categories")
    public String categoriesPage(Model model) {
        model.addAttribute("currentPage", "admin-catalog");
        model.addAttribute("breadcrumb", "Kategori Produk");
        model.addAttribute("categoryTypes", CategoryType.values());

        // SSR: inject initial data for faster first paint
        List<CategoryDTO> categories = manageCategoriesUseCase.findAllForAdmin().stream()
                .map(this::toCategoryDTO).toList();
        model.addAttribute("initialCategories", categories);

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

        // SSR: inject initial data
        List<CategoryDTO> categories = manageCategoriesUseCase.findAllForAdmin().stream()
                .map(this::toCategoryDTO).toList();
        model.addAttribute("initialCategories", categories);

        List<Products> products;
        if (categoryId != null && !categoryId.isEmpty()) {
            products = manageProductsUseCase.findByCategoryForAdmin(UUID.fromString(categoryId));
        } else {
            products = manageCategoriesUseCase.findAllForAdmin().stream()
                    .flatMap(cat -> manageProductsUseCase.findByCategoryForAdmin(cat.getId()).stream())
                    .toList();
        }
        model.addAttribute("initialProducts", products.stream().map(this::toProductDTO).toList());

        return "pages/admin/catalog/products";
    }

    @GetMapping("/products/{productId}/denoms")
    public String denomsPage(@PathVariable UUID productId, Model model) {
        model.addAttribute("currentPage", "admin-catalog");
        model.addAttribute("breadcrumb", "Denominasi");
        model.addAttribute("productId", productId);
        model.addAttribute("denomTypes", DenomType.values());

        // SSR: inject initial data
        List<ProductDenomDTO> denoms = manageDenomsUseCase.findByProductForAdmin(productId).stream()
                .map(this::toDenomDTO).toList();
        model.addAttribute("initialDenoms", denoms);

        manageProductsUseCase.findById(productId).ifPresent(prod -> {
            model.addAttribute("initialProduct", toProductDTO(prod));
        });

        return "pages/admin/catalog/denoms";
    }

    // ==================== Mappers ====================

    private CategoryDTO toCategoryDTO(Category entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setCategoryType(entity.getCategoryType());
        dto.setIconUrl(entity.getIconUrl());
        dto.setSortOrder(entity.getSortOrder());
        dto.setActive(entity.isActive());
        dto.setDeleted(entity.isDeleted());
        return dto;
    }

    private ProductDTO toProductDTO(Products entity) {
        ProductDTO dto = new ProductDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setProviderName(entity.getProviderName());
        dto.setDescription(entity.getDescription());
        dto.setIconUrl(entity.getIconUrl());
        dto.setSortOrder(entity.getSortOrder());
        dto.setActive(entity.isActive());
        dto.setDeleted(entity.isDeleted());
        dto.setCategoryId(entity.getCategoryId());
        return dto;
    }

    private ProductDenomDTO toDenomDTO(ProductDenoms entity) {
        ProductDenomDTO dto = new ProductDenomDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDenomType(entity.getDenomType());
        dto.setNominal(entity.getNominal());
        dto.setPrice(entity.getPrice());
        dto.setBasePrice(entity.getBasePrice());
        dto.setAdminFee(entity.getAdminFee());
        dto.setValidityDays(entity.getValidityDays());
        dto.setQuotaMb(entity.getQuotaMb());
        dto.setMinAmount(entity.getMinAmount());
        dto.setMaxAmount(entity.getMaxAmount());
        dto.setRequiresInquiry(entity.isRequiresInquiry());
        dto.setStockAvailable(entity.getStockAvailable());
        dto.setSortOrder(entity.getSortOrder());
        dto.setActive(entity.isActive());
        dto.setDeleted(entity.isDeleted());
        dto.setProductId(entity.getProductId());
        return dto;
    }
}
