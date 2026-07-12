package com.satset.catalog.web;

import com.satset.catalog.dto.BulkPriceUpdateRequest;
import com.satset.catalog.dto.CategoryDTO;
import com.satset.catalog.dto.DenomListItemDTO;
import com.satset.catalog.dto.PriceUpdateResult;
import com.satset.catalog.dto.ProductDTO;
import com.satset.catalog.dto.ProductDenomDTO;
import com.satset.catalog.model.Category;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.dto.CreateCategoryRequest;
import com.satset.catalog.dto.CreateDenomRequest;
import com.satset.catalog.dto.CreateProductRequest;
import com.satset.catalog.dto.UpdateCategoryRequest;
import com.satset.catalog.dto.UpdateDenomRequest;
import com.satset.catalog.dto.UpdateProductRequest;
import com.satset.catalog.service.CategoryDomainService;
import com.satset.catalog.service.DenomDomainService;
import com.satset.catalog.service.ProductDomainService;
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
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<CategoryDTO>> listCategories() {
        List<CategoryDTO> dtos = manageCategoriesUseCase.findAllForAdmin().stream()
                .map(CatalogDtoMapper::toCategoryDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/categories/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<CategoryDTO> getCategory(@PathVariable UUID id) {
        return manageCategoriesUseCase.findById(id)
                .map(CatalogDtoMapper::toCategoryDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public ResponseEntity<?> createCategory(@Valid @RequestBody CreateCategoryRequest req)
            throws BusinessException {
        Category created = manageCategoriesUseCase.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(CatalogDtoMapper.toCategoryDTO(created));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public ResponseEntity<?> updateCategory(@PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest req) throws BusinessException {
        Category updated = manageCategoriesUseCase.update(id, req);
        return ResponseEntity.ok(CatalogDtoMapper.toCategoryDTO(updated));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_CATEGORIES + "')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) throws BusinessException {
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
        List<ProductDTO> dtos = products.stream().map(CatalogDtoMapper::toProductDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable UUID id) {
        return manageProductsUseCase.findById(id)
                .map(CatalogDtoMapper::toProductDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public ResponseEntity<?> createProduct(@Valid @RequestBody CreateProductRequest req)
            throws BusinessException {
        Products created = manageProductsUseCase.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(CatalogDtoMapper.toProductDTO(created));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_PRODUCTS + "')")
    public ResponseEntity<?> updateProduct(@PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest req) throws BusinessException {
        Products updated = manageProductsUseCase.update(id, req);
        return ResponseEntity.ok(CatalogDtoMapper.toProductDTO(updated));
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
                .map(CatalogDtoMapper::toDenomDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/denoms")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<List<DenomListItemDTO>> listAllDenoms() {
        return ResponseEntity.ok(manageDenomsUseCase.findAllForList());
    }

    @GetMapping("/denoms/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_VIEW_CATALOG + "')")
    public ResponseEntity<ProductDenomDTO> getDenom(@PathVariable UUID id) {
        return manageDenomsUseCase.findById(id)
                .map(CatalogDtoMapper::toDenomDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/products/{productId}/denoms")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<?> createDenom(@PathVariable UUID productId,
            @Valid @RequestBody CreateDenomRequest req) throws BusinessException {
        ProductDenoms created = manageDenomsUseCase.create(productId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(CatalogDtoMapper.toDenomDTO(created));
    }

    @PutMapping("/denoms/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<?> updateDenom(@PathVariable UUID id,
            @Valid @RequestBody UpdateDenomRequest req) throws BusinessException {
        ProductDenoms updated = manageDenomsUseCase.update(id, req);
        return ResponseEntity.ok(CatalogDtoMapper.toDenomDTO(updated));
    }

    @PutMapping("/denoms/prices")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<List<PriceUpdateResult>> updateDenomPrices(
            @RequestBody List<BulkPriceUpdateRequest> req) {
        return ResponseEntity.ok(manageDenomsUseCase.updatePrices(req));
    }

    @DeleteMapping("/denoms/{id}")
    @PreAuthorize("hasRole('" + OmniConstants.PERM_MANAGE_DENOMS + "')")
    public ResponseEntity<Void> deleteDenom(@PathVariable UUID id) {
        manageDenomsUseCase.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
