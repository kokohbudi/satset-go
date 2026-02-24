package com.omnip.catalog.domain.service;

import com.omnip.catalog.domain.model.Categories;
import com.omnip.catalog.domain.model.Products;
import com.omnip.catalog.domain.port.in.BrowseProductsUseCase;
import com.omnip.catalog.adapter.out.persistence.CategoryJpaRepository;
import com.omnip.catalog.adapter.out.persistence.ProductJpaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductDomainService implements BrowseProductsUseCase {

    private final ProductJpaRepository productRepository;
    private final CategoryJpaRepository categoryRepository;

    public ProductDomainService(ProductJpaRepository productRepository, CategoryJpaRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Products> findByCategory(String categoryCode) {
        Optional<Categories> category = categoryRepository.findByCode(categoryCode);
        if (category.isEmpty()) {
            return List.of();
        }
        return productRepository.findByCategoryIdAndActiveTrueAndDeletedFalseOrderBySortOrder(category.get().getId());
    }

    @Override
    @Cacheable(value = "products", cacheManager = "standardCacheManager")
    public List<Products> findActiveProducts() {
        return productRepository.findByActiveTrueAndDeletedFalseOrderBySortOrder();
    }

    @Override
    public Optional<Products> findByCode(String code) {
        return productRepository.findByCode(code)
                .filter(p -> p.isActive() && !p.isDeleted());
    }
}