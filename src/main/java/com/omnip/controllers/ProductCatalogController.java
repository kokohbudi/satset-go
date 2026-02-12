package com.omnip.controllers;

import com.omnip.dtos.CategoryDTO;
import com.omnip.dtos.ProductDTO;
import com.omnip.dtos.ProductDenomDTO;
import com.omnip.enums.CategoryType;
import com.omnip.services.CategoryService;
import com.omnip.services.ProductDenomService;
import com.omnip.services.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductCatalogController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final ProductDenomService denomService;

    public ProductCatalogController(CategoryService categoryService,
                                    ProductService productService,
                                    ProductDenomService denomService) {
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
