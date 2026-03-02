package com.omnip.catalog.adapter.in.web;

import com.omnip.catalog.adapter.in.web.dto.CategoryDTO;
import com.omnip.catalog.adapter.in.web.dto.ProductDTO;
import com.omnip.catalog.adapter.in.web.dto.ProductDenomDTO;
import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.CategoryType;
import com.omnip.catalog.domain.model.DenomType;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.model.Products;
import com.omnip.catalog.domain.port.in.ManageCategoriesUseCase;
import com.omnip.catalog.domain.port.in.ManageDenomsUseCase;
import com.omnip.catalog.domain.port.in.ManageProductsUseCase;
import com.omnip.shared.constant.OmniConstants;
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

    private final ManageCategoriesUseCase manageCategoriesUseCase;
    private final ManageProductsUseCase manageProductsUseCase;
    private final ManageDenomsUseCase manageDenomsUseCase;

    public AdminCatalogPageController(ManageCategoriesUseCase manageCategoriesUseCase,
                                      ManageProductsUseCase manageProductsUseCase,
                                      ManageDenomsUseCase manageDenomsUseCase) {
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

    private CategoryDTO toCategoryDTO(Categories entity) {
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
        if (entity.getCategory() != null) {
            dto.setCategoryId(entity.getCategory().getId());
            dto.setCategoryCode(entity.getCategory().getCode());
            dto.setCategoryName(entity.getCategory().getName());
        }
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
        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getId());
            dto.setProductCode(entity.getProduct().getCode());
            dto.setProductName(entity.getProduct().getName());
        }
        return dto;
    }
}
