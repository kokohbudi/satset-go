package com.omnip.catalog.adapter.in.web;

import com.omnip.catalog.adapter.in.web.dto.CategoryDTO;
import com.omnip.catalog.adapter.in.web.dto.ProductDTO;
import com.omnip.catalog.adapter.in.web.dto.ProductDenomDTO;
import com.omnip.catalog.domain.model.CategoryType;
import com.omnip.catalog.domain.service.CategoryDomainService;
import com.omnip.catalog.domain.service.DenomDomainService;
import com.omnip.catalog.domain.service.ProductDomainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductCatalogController {

    private final CategoryDomainService categoryService;
    private final ProductDomainService productService;
    private final DenomDomainService denomService;

    public ProductCatalogController(CategoryDomainService categoryService,
                                    ProductDomainService productService,
                                    DenomDomainService denomService) {
        this.categoryService = categoryService;
        this.productService = productService;
        this.denomService = denomService;
    }

    // ==================== Categories ====================

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/categories/type/{type}")
    public ResponseEntity<List<CategoryDTO>> getCategoriesByType(@PathVariable CategoryType type) {
        return ResponseEntity.ok(categoryService.findByType(type));
    }

    @GetMapping("/categories/{code}")
    public ResponseEntity<CategoryDTO> getCategoryByCode(@PathVariable String code) {
        return categoryService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Products ====================

    @GetMapping("/categories/{code}/products")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String code) {
        return ResponseEntity.ok(productService.findByCategory(code));
    }

    @GetMapping("/products/{code}")
    public ResponseEntity<ProductDTO> getProductByCode(@PathVariable String code) {
        return productService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Denominations ====================

    @GetMapping("/products/{code}/denoms")
    public ResponseEntity<List<ProductDenomDTO>> getDenomsByProduct(@PathVariable String code) {
        return ResponseEntity.ok(denomService.findByProduct(code));
    }

    @GetMapping("/denoms/{code}")
    public ResponseEntity<ProductDenomDTO> getDenomByCode(@PathVariable String code) {
        return denomService.getDenomWithMeta(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
