package com.omnip.services;

import com.omnip.dtos.ProductDTO;
import com.omnip.entities.Categories;
import com.omnip.entities.Products;
import com.omnip.repositories.CategoryRepository;
import com.omnip.repositories.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductDTO> findByCategory(String categoryCode) {
        Optional<Categories> category = categoryRepository.findByCode(categoryCode);
        if (category.isEmpty()) {
            return List.of();
        }
        return productRepository.findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(category.get().getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Cacheable(value = "products", cacheManager = "standardCacheManager")
    public List<ProductDTO> findActiveProducts() {
        return productRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<ProductDTO> findByCode(String code) {
        return productRepository.findByCode(code)
                .filter(p -> p.isActive() && !p.isDeleted())
                .map(this::toDTO);
    }

    private ProductDTO toDTO(Products entity) {
        ProductDTO dto = new ProductDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setProviderName(entity.getProviderName());
        dto.setDescription(entity.getDescription());
        dto.setIconUrl(entity.getIconUrl());
        if (entity.getCategory() != null) {
            dto.setCategoryCode(entity.getCategory().getCode());
            dto.setCategoryName(entity.getCategory().getName());
        }
        return dto;
    }
}