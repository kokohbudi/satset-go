package com.satset.catalog.web;

import com.satset.catalog.model.*;
import com.satset.catalog.service.category.CategoryDomainService;
import com.satset.catalog.service.denom.DenomDomainService;
import com.satset.catalog.service.product.ProductDomainService;
import com.satset.shared.constant.SatsetConstants;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('" + SatsetConstants.PERM_PURCHASE + "')")
public class ProductCatalogController {

    private final CategoryDomainService browseCategoriesUseCase;
    private final ProductDomainService browseProductsUseCase;
    private final DenomDomainService browseDenomsUseCase;

    public ProductCatalogController(CategoryDomainService browseCategoriesUseCase,
            ProductDomainService browseProductsUseCase,
            DenomDomainService browseDenomsUseCase) {
        this.browseCategoriesUseCase = browseCategoriesUseCase;
        this.browseProductsUseCase = browseProductsUseCase;
        this.browseDenomsUseCase = browseDenomsUseCase;
    }

    // ==================== Categories ====================

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(browseCategoriesUseCase.findAll());
    }

    @GetMapping("/categories/type/{type}")
    public ResponseEntity<List<Category>> getCategoriesByType(@PathVariable CategoryType type) {
        return ResponseEntity.ok(browseCategoriesUseCase.findByType(type));
    }

    @GetMapping("/categories/{code}")
    public ResponseEntity<Category> getCategoryByCode(@PathVariable String code) {
        return browseCategoriesUseCase.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Products ====================

    @GetMapping("/categories/{code}/products")
    public ResponseEntity<List<Products>> getProductsByCategory(@PathVariable String code) {
        return ResponseEntity.ok(browseProductsUseCase.findByCategory(code));
    }

    @GetMapping("/categories/{catCode}/products/{prodCode}")
    public ResponseEntity<Products> getProductByCategoryAndCode(
            @PathVariable String catCode, @PathVariable String prodCode) {
        return browseProductsUseCase.findByCategoryAndCode(catCode, prodCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Denominations ====================

    @GetMapping("/categories/{catCode}/products/{prodCode}/denoms")
    public ResponseEntity<List<ProductDenoms>> getDenomsByCategoryAndProduct(
            @PathVariable String catCode, @PathVariable String prodCode) {
        return ResponseEntity.ok(browseDenomsUseCase.findByProduct(catCode, prodCode));
    }

    @GetMapping("/denoms/{code}")
    public ResponseEntity<ProductDenoms> getDenomByCode(@PathVariable String code) {
        return browseDenomsUseCase.getDenomWithMeta(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
