package com.satset.catalog.adapter.in.web;

import com.satset.catalog.adapter.in.web.dto.CategoryDTO;
import com.satset.catalog.adapter.in.web.dto.ProductDTO;
import com.satset.catalog.adapter.in.web.dto.ProductDenomDTO;
import com.satset.catalog.adapter.in.web.dto.ProductDenomMetaDTO;
import com.satset.catalog.domain.model.*;
import com.satset.catalog.domain.port.in.BrowseCategoriesUseCase;
import com.satset.catalog.domain.port.in.BrowseDenomsUseCase;
import com.satset.catalog.domain.port.in.BrowseProductsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductCatalogController {

    private final BrowseCategoriesUseCase browseCategoriesUseCase;
    private final BrowseProductsUseCase browseProductsUseCase;
    private final BrowseDenomsUseCase browseDenomsUseCase;

    public ProductCatalogController(BrowseCategoriesUseCase browseCategoriesUseCase,
            BrowseProductsUseCase browseProductsUseCase,
            BrowseDenomsUseCase browseDenomsUseCase) {
        this.browseCategoriesUseCase = browseCategoriesUseCase;
        this.browseProductsUseCase = browseProductsUseCase;
        this.browseDenomsUseCase = browseDenomsUseCase;
    }

    // ==================== Categories ====================

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> dtos = browseCategoriesUseCase.findAll().stream()
                .map(this::toCategoryDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/categories/type/{type}")
    public ResponseEntity<List<CategoryDTO>> getCategoriesByType(@PathVariable CategoryType type) {
        List<CategoryDTO> dtos = browseCategoriesUseCase.findByType(type).stream()
                .map(this::toCategoryDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/categories/{code}")
    public ResponseEntity<CategoryDTO> getCategoryByCode(@PathVariable String code) {
        return browseCategoriesUseCase.findByCode(code)
                .map(this::toCategoryDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Products ====================

    @GetMapping("/categories/{code}/products")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String code) {
        List<ProductDTO> dtos = browseProductsUseCase.findByCategory(code).stream()
                .map(this::toProductDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/products/{code}")
    public ResponseEntity<ProductDTO> getProductByCode(@PathVariable String code) {
        return browseProductsUseCase.findByCode(code)
                .map(this::toProductDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Denominations ====================

    @GetMapping("/products/{code}/denoms")
    public ResponseEntity<List<ProductDenomDTO>> getDenomsByProduct(@PathVariable String code) {
        List<ProductDenomDTO> dtos = browseDenomsUseCase.findByProduct(code).stream()
                .map(this::toDenomDTO).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/denoms/{code}")
    public ResponseEntity<ProductDenomDTO> getDenomByCode(@PathVariable String code) {
        return browseDenomsUseCase.getDenomWithMeta(code)
                .map(this::toDenomDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
        if (entity.getMetadata() != null) {
            dto.setMetadata(entity.getMetadata().stream().map(this::toMetaDTO).toList());
        }
        return dto;
    }

    private ProductDenomMetaDTO toMetaDTO(ProductDenomMeta entity) {
        ProductDenomMetaDTO dto = new ProductDenomMetaDTO();
        dto.setMetaKey(entity.getMetaKey());
        dto.setMetaValue(entity.getMetaValue());
        return dto;
    }
}
