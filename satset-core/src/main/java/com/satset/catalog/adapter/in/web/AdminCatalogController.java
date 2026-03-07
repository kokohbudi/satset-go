package com.satset.catalog.adapter.in.web;

import com.satset.catalog.adapter.in.web.dto.CategoryDTO;
import com.satset.catalog.adapter.in.web.dto.ProductDTO;
import com.satset.catalog.adapter.in.web.dto.ProductDenomDTO;
import com.satset.catalog.domain.model.Category;
import com.satset.catalog.domain.model.ProductDenoms;
import com.satset.catalog.domain.model.Products;
import com.satset.catalog.domain.port.in.*;
import com.satset.shared.constant.OmniConstants;
import com.satset.shared.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/catalog")
@PreAuthorize("isAuthenticated()")
public class AdminCatalogController {

    private final ManageCategoriesUseCase manageCategoriesUseCase;
    private final ManageProductsUseCase manageProductsUseCase;
    private final ManageDenomsUseCase manageDenomsUseCase;

    public AdminCatalogController(ManageCategoriesUseCase manageCategoriesUseCase,
                                  ManageProductsUseCase manageProductsUseCase,
                                  ManageDenomsUseCase manageDenomsUseCase) {
        this.manageCategoriesUseCase = manageCategoriesUseCase;
        this.manageProductsUseCase = manageProductsUseCase;
        this.manageDenomsUseCase = manageDenomsUseCase;
    }

    // ==================== Categories ====================

    @GetMapping("/categories")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<CategoryDTO>> listCategories() {
        List<CategoryDTO> dtos = manageCategoriesUseCase.findAllForAdmin().stream()
                .map(this::toCategoryDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<CategoryDTO> getCategory(@PathVariable UUID id) {
        return manageCategoriesUseCase.findById(id)
                .map(this::toCategoryDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CreateCategoryRequest req)
            throws BusinessException {
        Category created = manageCategoriesUseCase.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCategoryDTO(created));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public ResponseEntity<?> updateCategory(@PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest req) throws BusinessException {
        Category updated = manageCategoriesUseCase.update(id, req);
        return ResponseEntity.ok(toCategoryDTO(updated));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        manageCategoriesUseCase.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Products ====================

    @GetMapping("/products")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<ProductDTO>> listProducts(
            @RequestParam(required = false) UUID categoryId) {
        List<Products> products;
        if (categoryId != null) {
            products = manageProductsUseCase.findByCategoryForAdmin(categoryId);
        } else {
            products = manageCategoriesUseCase.findAllForAdmin().stream()
                    .flatMap(cat -> manageProductsUseCase.findByCategoryForAdmin(cat.getId()).stream())
                    .toList();
        }
        List<ProductDTO> dtos = products.stream().map(this::toProductDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable UUID id) {
        return manageProductsUseCase.findById(id)
                .map(this::toProductDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public ResponseEntity<?> createProduct(@Valid @RequestBody CreateProductRequest req)
            throws BusinessException {
        Products created = manageProductsUseCase.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toProductDTO(created));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest req) throws BusinessException {
        Products updated = manageProductsUseCase.update(id, req);
        return ResponseEntity.ok(toProductDTO(updated));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        manageProductsUseCase.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Denoms ====================

    @GetMapping("/products/{productId}/denoms")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<ProductDenomDTO>> listDenoms(@PathVariable UUID productId) {
        List<ProductDenomDTO> dtos = manageDenomsUseCase.findByProductForAdmin(productId).stream()
                .map(this::toDenomDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/denoms/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<ProductDenomDTO> getDenom(@PathVariable UUID id) {
        return manageDenomsUseCase.findById(id)
                .map(this::toDenomDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products/{productId}/denoms")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<?> createDenom(@PathVariable UUID productId,
            @Valid @RequestBody CreateDenomRequest req) throws BusinessException {
        ProductDenoms created = manageDenomsUseCase.create(productId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDenomDTO(created));
    }

    @PutMapping("/denoms/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<?> updateDenom(@PathVariable UUID id,
            @Valid @RequestBody UpdateDenomRequest req) throws BusinessException {
        ProductDenoms updated = manageDenomsUseCase.update(id, req);
        return ResponseEntity.ok(toDenomDTO(updated));
    }

    @DeleteMapping("/denoms/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<Void> deleteDenom(@PathVariable UUID id) {
        manageDenomsUseCase.softDelete(id);
        return ResponseEntity.noContent().build();
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
