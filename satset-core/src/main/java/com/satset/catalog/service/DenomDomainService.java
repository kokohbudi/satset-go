package com.satset.catalog.service;

import com.satset.catalog.repository.CategoryRepository;
import com.satset.catalog.repository.DenomMetaRepository;
import com.satset.catalog.repository.DenomRepository;
import com.satset.catalog.repository.ProductRepository;
import com.satset.catalog.model.Category;
import com.satset.catalog.model.DenomType;
import com.satset.catalog.model.ProductDenomMeta;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;
import com.satset.catalog.dto.CreateDenomRequest;
import com.satset.catalog.dto.UpdateDenomRequest;
import com.satset.shared.exception.BusinessException;
import com.satset.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DenomDomainService {

    private final DenomRepository denomRepository;
    private final DenomMetaRepository metaRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public DenomDomainService(DenomRepository denomRepository,
            DenomMetaRepository metaRepository,
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        this.denomRepository = denomRepository;
        this.metaRepository = metaRepository;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // === Browse (read-only) ===

    public List<ProductDenoms> findByProduct(String categoryCode, String productCode) {
        Optional<Category> category = categoryRepository.findByCode(categoryCode);
        if (category.isEmpty()) return List.of();
        Optional<Products> product =
                productRepository.findByCategoryIdAndCode(category.get().getId(), productCode);
        if (product.isEmpty()) return List.of();
        return denomRepository
                .findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(product.get().getId());
    }

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

    public List<ProductDenoms> findByProductForAdmin(UUID productId) {
        return denomRepository.findByProductIdOrderBySortOrder(productId);
    }

    public Optional<ProductDenoms> findById(UUID id) {
        return denomRepository.findById(id);
    }

    @Transactional
    public ProductDenoms create(UUID productId, CreateDenomRequest req) throws BusinessException {
        Products product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        String code = (req.code() == null || req.code().isBlank())
            ? generateSku(product, req.nominal())
            : req.code().toUpperCase().trim();
        if (denomRepository.findByCode(code).isPresent()) {
            throw new BusinessException("DUPLICATE_CODE", "Denom code already exists: " + code);
        }
        ProductDenoms denom = new ProductDenoms();
        denom.setProductId(product.getId());
        denom.setCode(code);
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

    /**
     * SKU otomatis: PRODUCT+NOMINAL tanpa dash (mis. TELKOMSEL10000). Open amount (nominal null)
     * pakai nomor urut (TELKOMSEL1). Bentrok → tambah angka di belakang.
     * ponytail: pada bentrok, suffix numerik bisa ambigu (TELKOMSEL10000 + 2 = TELKOMSEL100002);
     * kasus langka, tambahkan separator kalau ini pernah jadi masalah.
     */
    private String generateSku(Products product, BigDecimal nominal) {
        String base = product.getCode();
        String candidate = nominal != null ? base + nominal.toBigInteger() : base + "1";
        int n = 2;
        while (denomRepository.findByCode(candidate).isPresent()) {
            candidate = (nominal != null ? base + nominal.toBigInteger() : base) + n++;
        }
        return candidate;
    }

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

    @Transactional
    public void softDelete(UUID id) {
        ProductDenoms denom = denomRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Denom", id));
        denom.setDeleted(true);
        denom.setActive(false);
        denomRepository.save(denom);
    }

    // === Supplier sync (Digiflazz) ===

    public List<ProductDenoms> findActiveByProductId(UUID productId) {
        return denomRepository.findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(productId);
    }

    @Transactional
    public ProductDenoms createFromSupplier(UUID productId, String sku, String name, BigDecimal cost) {
        // revive kalau code (soft-deleted) sudah ada — hindari UNIQUE violation + biar gak muncul terus di sync
        ProductDenoms d = denomRepository.findByCode(sku).orElseGet(ProductDenoms::new);
        d.setProductId(productId);
        d.setCode(sku);                 // apa adanya — JANGAN uppercase
        d.setName(name);
        d.setDenomType(DenomType.FIXED_DENOM);
        d.setBasePrice(cost);
        d.setActive(true);
        d.setDeleted(false);
        return denomRepository.save(d);
    }

    @Transactional
    public void updateCostById(UUID denomId, BigDecimal cost) {
        ProductDenoms d = denomRepository.findById(denomId)
                .orElseThrow(() -> new ResourceNotFoundException("Denom", denomId));
        d.setBasePrice(cost);
        denomRepository.save(d);
    }

    @Transactional
    public void deactivateById(UUID denomId) {
        ProductDenoms d = denomRepository.findById(denomId)
                .orElseThrow(() -> new ResourceNotFoundException("Denom", denomId));
        d.setActive(false);
        denomRepository.save(d);
    }

    /** Set flag inSupplier tiap denom produk: true kalau code (uppercase) ada di {@code supplierCodesUpper}. */
    @Transactional
    public int reconcileSupplierFlags(UUID productId, java.util.Set<String> supplierCodesUpper) {
        int changed = 0;
        for (ProductDenoms d : denomRepository.findByProductIdOrderBySortOrder(productId)) {
            if (d.isDeleted()) continue;
            boolean present = supplierCodesUpper.contains(d.getCode().toUpperCase());
            if (d.isInSupplier() != present) { d.setInSupplier(present); denomRepository.save(d); changed++; }
        }
        return changed;
    }
}