package com.satset.catalog.domain.service;

import com.satset.catalog.domain.model.ProductDenomMeta;
import com.satset.catalog.domain.model.ProductDenoms;
import com.satset.catalog.domain.model.Products;
import com.satset.catalog.domain.port.in.BrowseDenomsUseCase;
import com.satset.catalog.domain.port.in.CreateDenomRequest;
import com.satset.catalog.domain.port.in.ManageDenomsUseCase;
import com.satset.catalog.domain.port.in.UpdateDenomRequest;
import com.satset.catalog.domain.port.out.DenomMetaRepositoryPort;
import com.satset.catalog.domain.port.out.DenomRepositoryPort;
import com.satset.catalog.domain.port.out.ProductRepositoryPort;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DenomDomainService implements BrowseDenomsUseCase, ManageDenomsUseCase {

    private final DenomRepositoryPort denomRepository;
    private final DenomMetaRepositoryPort metaRepository;
    private final ProductRepositoryPort productRepository;

    public DenomDomainService(DenomRepositoryPort denomRepository,
            DenomMetaRepositoryPort metaRepository,
            ProductRepositoryPort productRepository) {
        this.denomRepository = denomRepository;
        this.metaRepository = metaRepository;
        this.productRepository = productRepository;
    }

    // === Browse (read-only) ===

    @Override
    public List<ProductDenoms> findByProduct(String productCode) {
        Optional<Products> product = productRepository.findByCode(productCode);
        if (product.isEmpty()) {
            return List.of();
        }
        return denomRepository.findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(product.get().getId());
    }

    @Override
    public Optional<ProductDenoms> findByCode(String code) {
        return denomRepository.findByCode(code)
                .filter(d -> d.isActive() && !d.isDeleted());
    }

    @Override
    public Optional<ProductDenoms> getDenomWithMeta(String code) {
        return denomRepository.findByCode(code)
                .filter(d -> d.isActive() && !d.isDeleted())
                .map(denom -> {
                    // Eagerly load metadata
                    List<ProductDenomMeta> metaList = metaRepository.findByProductDenomId(denom.getId());
                    denom.setMetadata(metaList);
                    return denom;
                });
    }

    // === Manage (admin CRUD) ===

    @Override
    public List<ProductDenoms> findByProductForAdmin(UUID productId) {
        return denomRepository.findByProductIdOrderBySortOrder(productId);
    }

    @Override
    public Optional<ProductDenoms> findById(UUID id) {
        return denomRepository.findById(id);
    }

    @Override
    @Transactional
    public ProductDenoms create(UUID productId, CreateDenomRequest req) throws BusinessException {
        Products product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (denomRepository.findByCode(req.code().toUpperCase().trim()).isPresent()) {
            throw new BusinessException("DUPLICATE_CODE", "Denom code already exists: " + req.code());
        }
        ProductDenoms denom = new ProductDenoms();
        denom.setProductId(product.getId());
        denom.setCode(req.code().toUpperCase().trim());
        denom.setName(req.name());
        denom.setDenomType(req.denomType());
        denom.setNominal(req.nominal());
        denom.setPrice(req.price());
        denom.setBasePrice(req.basePrice());
        denom.setAdminFee(req.adminFee());
        denom.setValidityDays(req.validityDays());
        denom.setQuotaMb(req.quotaMb());
        denom.setMinAmount(req.minAmount());
        denom.setMaxAmount(req.maxAmount());
        denom.setRequiresInquiry(req.requiresInquiry());
        denom.setStockAvailable(req.stockAvailable());
        denom.setActive(req.active());
        denom.setSortOrder(req.sortOrder());
        denom.setDeleted(false);
        return denomRepository.save(denom);
    }

    @Override
    @Transactional
    public ProductDenoms update(UUID id, UpdateDenomRequest req) throws BusinessException {
        ProductDenoms denom = denomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Denom", id));
        if (denomRepository.existsByCodeAndIdNot(req.code().toUpperCase().trim(), id)) {
            throw new BusinessException("DUPLICATE_CODE", "Denom code already exists: " + req.code());
        }
        denom.setCode(req.code().toUpperCase().trim());
        denom.setName(req.name());
        denom.setDenomType(req.denomType());
        denom.setNominal(req.nominal());
        denom.setPrice(req.price());
        denom.setBasePrice(req.basePrice());
        denom.setAdminFee(req.adminFee());
        denom.setValidityDays(req.validityDays());
        denom.setQuotaMb(req.quotaMb());
        denom.setMinAmount(req.minAmount());
        denom.setMaxAmount(req.maxAmount());
        denom.setRequiresInquiry(req.requiresInquiry());
        denom.setStockAvailable(req.stockAvailable());
        denom.setActive(req.active());
        denom.setSortOrder(req.sortOrder());
        return denomRepository.save(denom);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        ProductDenoms denom = denomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Denom", id));
        denom.setDeleted(true);
        denom.setActive(false);
        denomRepository.save(denom);
    }
}