package com.satset.catalog.web;

import com.satset.catalog.dto.BulkNameUpdateRequest;
import com.satset.catalog.dto.BulkPriceUpdateRequest;
import com.satset.catalog.dto.PriceUpdateResult;
import com.satset.catalog.model.Category;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.dto.CreateCategoryRequest;
import com.satset.catalog.dto.CreateDenomRequest;
import com.satset.catalog.dto.CreateProductRequest;
import com.satset.catalog.dto.UpdateCategoryRequest;
import com.satset.catalog.dto.UpdateDenomRequest;
import com.satset.catalog.dto.UpdateProductRequest;
import com.satset.catalog.service.category.CategoryDomainService;
import com.satset.catalog.service.denom.DenomDomainService;
import com.satset.catalog.service.product.ProductDomainService;
import com.satset.shared.constant.SatsetConstants;
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

    private final CategoryDomainService manageCategoriesUseCase;
    private final ProductDomainService manageProductsUseCase;
    private final DenomDomainService manageDenomsUseCase;

    public AdminCatalogController(CategoryDomainService manageCategoriesUseCase,
                                  ProductDomainService manageProductsUseCase,
                                  DenomDomainService manageDenomsUseCase) {
        this.manageCategoriesUseCase = manageCategoriesUseCase;
        this.manageProductsUseCase = manageProductsUseCase;
        this.manageDenomsUseCase = manageDenomsUseCase;
    }

    // ==================== Categories ====================

    @GetMapping("/categories")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<Category>> listCategories() {
        return ResponseEntity.ok(manageCategoriesUseCase.findAllForAdmin());
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<Category> getCategory(@PathVariable UUID id) {
        return manageCategoriesUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_CATEGORIES + "')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CreateCategoryRequest req)
            throws BusinessException {
        Category created = manageCategoriesUseCase.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_CATEGORIES + "')")
    public ResponseEntity<?> updateCategory(@PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest req) throws BusinessException {
        Category updated = manageCategoriesUseCase.update(id, req);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_CATEGORIES + "')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) throws BusinessException {
        manageCategoriesUseCase.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Products ====================

    @GetMapping("/products")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<Products>> listProducts(
            @RequestParam(required = false) UUID categoryId) {
        List<Products> products;
        if (categoryId != null) {
            products = manageProductsUseCase.findByCategoryForAdmin(categoryId);
        } else {
            products = manageProductsUseCase.findAllForAdmin();
        }
        return ResponseEntity.ok(products);
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<Products> getProduct(@PathVariable UUID id) {
        return manageProductsUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_PRODUCTS + "')")
    public ResponseEntity<?> createProduct(@Valid @RequestBody CreateProductRequest req)
            throws BusinessException {
        Products created = manageProductsUseCase.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_PRODUCTS + "')")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest req) throws BusinessException {
        Products updated = manageProductsUseCase.update(id, req);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/products/names")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_PRODUCTS + "')")
    public ResponseEntity<List<PriceUpdateResult>> updateProductNames(
            @RequestBody List<BulkNameUpdateRequest> req) {
        return ResponseEntity.ok(manageProductsUseCase.updateNames(req));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_PRODUCTS + "')")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        manageProductsUseCase.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Denoms ====================

    @GetMapping("/denoms")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<ProductDenoms>> listAllDenoms() {
        return ResponseEntity.ok(manageDenomsUseCase.findAllForAdmin());
    }

    @GetMapping("/products/{productId}/denoms")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<ProductDenoms>> listDenoms(@PathVariable UUID productId) {
        return ResponseEntity.ok(manageDenomsUseCase.findByProductForAdmin(productId));
    }

    @GetMapping("/denoms/{id}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<ProductDenoms> getDenom(@PathVariable UUID id) {
        return manageDenomsUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products/{productId}/denoms")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<?> createDenom(@PathVariable UUID productId,
            @Valid @RequestBody CreateDenomRequest req) throws BusinessException {
        ProductDenoms created = manageDenomsUseCase.create(productId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/denoms/{id}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<?> updateDenom(@PathVariable UUID id,
            @Valid @RequestBody UpdateDenomRequest req) throws BusinessException {
        ProductDenoms updated = manageDenomsUseCase.update(id, req);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/denoms/prices")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<List<PriceUpdateResult>> updateDenomPrices(
            @RequestBody List<BulkPriceUpdateRequest> req) {
        return ResponseEntity.ok(manageDenomsUseCase.updatePrices(req));
    }

    @PutMapping("/denoms/names")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<List<PriceUpdateResult>> updateDenomNames(
            @RequestBody List<BulkNameUpdateRequest> req) {
        return ResponseEntity.ok(manageDenomsUseCase.updateNames(req));
    }

    @DeleteMapping("/denoms/{id}")
    @PreAuthorize("hasRole('" + SatsetConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<Void> deleteDenom(@PathVariable UUID id) {
        manageDenomsUseCase.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
