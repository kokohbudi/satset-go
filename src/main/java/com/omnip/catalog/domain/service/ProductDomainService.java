package com.omnip.catalog.domain.service;

import com.omnip.catalog.domain.port.in.CreateProductRequest;
import com.omnip.catalog.domain.port.in.UpdateProductRequest;
import com.omnip.catalog.domain.model.Category;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.model.Products;
import com.omnip.catalog.domain.port.in.BrowseProductsUseCase;
import com.omnip.catalog.domain.port.in.ManageProductsUseCase;
import com.omnip.catalog.domain.port.out.CategoryRepositoryPort;
import com.omnip.catalog.domain.port.out.DenomRepositoryPort;
import com.omnip.catalog.domain.port.out.ProductRepositoryPort;
import com.omnip.shared.exception.BusinessException;
import com.omnip.shared.exception.ResourceNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductDomainService implements BrowseProductsUseCase, ManageProductsUseCase {

    private final ProductRepositoryPort productRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final DenomRepositoryPort denomRepository;

    public ProductDomainService(ProductRepositoryPort productRepository,
                                CategoryRepositoryPort categoryRepository,
                                DenomRepositoryPort denomRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.denomRepository = denomRepository;
    }

    // === Browse (read-only, cached) ===

    @Override
    public List<Products> findByCategory(String categoryCode) {
        Optional<Category> category = categoryRepository.findByCode(categoryCode);
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

    // === Manage (admin CRUD) ===

    @Override
    public List<Products> findByCategoryForAdmin(UUID categoryId) {
        return productRepository.findByCategoryIdOrderBySortOrder(categoryId);
    }

    @Override
    public Optional<Products> findById(UUID id) {
        return productRepository.findById(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager")
    public Products create(CreateProductRequest req) throws BusinessException {
        Category category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        if (productRepository.findByCode(req.code().toUpperCase().trim()).isPresent()) {
            throw new BusinessException("DUPLICATE_CODE", "Product code already exists: " + req.code());
        }
        Products product = new Products();
        product.setCategoryId(category.getId());
        product.setCode(req.code().toUpperCase().trim());
        product.setName(req.name());
        product.setProviderName(req.providerName());
        product.setDescription(req.description());
        product.setIconUrl(req.iconUrl());
        product.setActive(req.active());
        product.setSortOrder(req.sortOrder());
        product.setDeleted(false);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager")
    public Products update(UUID id, UpdateProductRequest req) throws BusinessException {
        Products product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        Category category = categoryRepository.findById(req.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category", req.categoryId()));
        if (productRepository.existsByCodeAndIdNot(req.code().toUpperCase().trim(), id)) {
            throw new BusinessException("DUPLICATE_CODE", "Product code already exists: " + req.code());
        }
        product.setCategoryId(category.getId());
        product.setCode(req.code().toUpperCase().trim());
        product.setName(req.name());
        product.setProviderName(req.providerName());
        product.setDescription(req.description());
        product.setIconUrl(req.iconUrl());
        product.setActive(req.active());
        product.setSortOrder(req.sortOrder());
        return productRepository.save(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true, cacheManager = "standardCacheManager")
    public void softDelete(UUID id) {
        Products product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        // Cascade: soft-delete all denoms belonging to this product
        List<ProductDenoms> denoms = denomRepository.findByProductIdOrderBySortOrder(product.getId());
        for (ProductDenoms denom : denoms) {
            if (!denom.isDeleted()) {
                denom.setDeleted(true);
                denom.setActive(false);
                denomRepository.save(denom);
            }
        }
        product.setDeleted(true);
        product.setActive(false);
        productRepository.save(product);
    }
}