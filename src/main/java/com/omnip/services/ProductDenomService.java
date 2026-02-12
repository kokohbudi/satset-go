package com.omnip.services;

import com.omnip.dtos.ProductDenomDTO;
import com.omnip.dtos.ProductDenomMetaDTO;
import com.omnip.entities.ProductDenomMeta;
import com.omnip.entities.ProductDenoms;
import com.omnip.entities.Products;
import com.omnip.repositories.ProductDenomMetaRepository;
import com.omnip.repositories.ProductDenomRepository;
import com.omnip.repositories.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProductDenomService {

    private final ProductDenomRepository denomRepository;
    private final ProductDenomMetaRepository metaRepository;
    private final ProductRepository productRepository;

    public ProductDenomService(ProductDenomRepository denomRepository,
                               ProductDenomMetaRepository metaRepository,
                               ProductRepository productRepository) {
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