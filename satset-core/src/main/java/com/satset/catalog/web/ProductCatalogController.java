package com.satset.catalog.web;

import com.satset.catalog.dto.CategoryDTO;
import com.satset.catalog.dto.ProductDTO;
import com.satset.catalog.dto.ProductDenomDTO;
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
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> dtos = browseCategoriesUseCase.findAll().stream()
                .map(CatalogDtoMapper::toCategoryDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/categories/type/{type}")
    public ResponseEntity<List<CategoryDTO>> getCategoriesByType(@PathVariable CategoryType type) {
        List<CategoryDTO> dtos = browseCategoriesUseCase.findByType(type).stream()
                .map(CatalogDtoMapper::toCategoryDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/categories/{code}")
    public ResponseEntity<CategoryDTO> getCategoryByCode(@PathVariable String code) {
        return browseCategoriesUseCase.findByCode(code)
                .map(CatalogDtoMapper::toCategoryDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Products ====================

    @GetMapping("/categories/{code}/products")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String code) {
        List<ProductDTO> dtos = browseProductsUseCase.findByCategory(code).stream()
                .map(CatalogDtoMapper::toProductDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/categories/{catCode}/products/{prodCode}")
    public ResponseEntity<ProductDTO> getProductByCategoryAndCode(
            @PathVariable String catCode, @PathVariable String prodCode) {
        return browseProductsUseCase.findByCategoryAndCode(catCode, prodCode)
                .map(CatalogDtoMapper::toProductDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Denominations ====================

    @GetMapping("/categories/{catCode}/products/{prodCode}/denoms")
    public ResponseEntity<List<ProductDenomDTO>> getDenomsByCategoryAndProduct(
            @PathVariable String catCode, @PathVariable String prodCode) {
        List<ProductDenomDTO> dtos = browseDenomsUseCase.findByProduct(catCode, prodCode).stream()
                .map(CatalogDtoMapper::toDenomDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/denoms/{code}")
    public ResponseEntity<ProductDenomDTO> getDenomByCode(@PathVariable String code) {
        return browseDenomsUseCase.getDenomWithMeta(code)
                .map(CatalogDtoMapper::toDenomDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
