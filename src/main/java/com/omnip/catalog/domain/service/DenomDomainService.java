package com.omnip.catalog.domain.service;

import com.omnip.catalog.adapter.in.web.dto.ProductDenomDTO;
import com.omnip.catalog.adapter.in.web.dto.ProductDenomMetaDTO;
import com.omnip.catalog.domain.model.ProductDenomMeta;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.model.Products;
import com.omnip.catalog.adapter.out.persistence.DenomMetaJpaRepository;
import com.omnip.catalog.adapter.out.persistence.DenomJpaRepository;
import com.omnip.catalog.adapter.out.persistence.ProductJpaRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DenomDomainService {

    private final DenomJpaRepository denomRepository;
    private final DenomMetaJpaRepository metaRepository;
    private final ProductJpaRepository productRepository;

    public DenomDomainService(DenomJpaRepository denomRepository,
                               DenomMetaJpaRepository metaRepository,
                               ProductJpaRepository productRepository) {
        this.denomRepository = denomRepository;
        this.metaRepository = metaRepository;
        this.productRepository = productRepository;
    }

    public List<ProductDenomDTO> findByProduct(String productCode) {
        Optional<Products> product = productRepository.findByCode(productCode);
        if (product.isEmpty()) {
            return List.of();
        }
        return denomRepository.findByProductIdAndActiveTrueAndDeletedFalseOrderBySortOrder(product.get().getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Optional<ProductDenomDTO> findByCode(String code) {
        return denomRepository.findByCode(code)
                .filter(d -> d.isActive() && !d.isDeleted())
                .map(this::toDTO);
    }

    public Optional<ProductDenomDTO> getDenomWithMeta(String code) {
        return denomRepository.findByCode(code)
                .filter(d -> d.isActive() && !d.isDeleted())
                .map(denom -> {
                    ProductDenomDTO dto = toDTO(denom);
                    List<ProductDenomMeta> metaList = metaRepository.findByProductDenomId(denom.getId());
                    dto.setMetadata(metaList.stream().map(this::toMetaDTO).toList());
                    return dto;
                });
    }

    private ProductDenomDTO toDTO(ProductDenoms entity) {
        ProductDenomDTO dto = new ProductDenomDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDenomType(entity.getDenomType());
        dto.setNominal(entity.getNominal());
        dto.setPrice(entity.getPrice());
        dto.setAdminFee(entity.getAdminFee());
        dto.setValidityDays(entity.getValidityDays());
        dto.setQuotaMb(entity.getQuotaMb());
        dto.setMinAmount(entity.getMinAmount());
        dto.setMaxAmount(entity.getMaxAmount());
        dto.setRequiresInquiry(entity.isRequiresInquiry());
        if (entity.getProduct() != null) {
            dto.setProductCode(entity.getProduct().getCode());
            dto.setProductName(entity.getProduct().getName());
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