package com.satset.catalog.adapter.out.persistence.mapper;

import com.satset.catalog.adapter.out.persistence.entity.ProductDenomJpaEntity;
import com.satset.catalog.domain.model.ProductDenoms;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ProductDenomMapper {

    public ProductDenoms toDomain(ProductDenomJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        ProductDenoms denom = new ProductDenoms();
        denom.setId(entity.getId());
        denom.setProductId(entity.getProductId());
        denom.setCode(entity.getCode());
        denom.setName(entity.getName());
        denom.setDenomType(entity.getDenomType());
        denom.setNominal(entity.getNominal());
        denom.setPrice(entity.getPrice());
        denom.setBasePrice(entity.getBasePrice());
        denom.setAdminFee(entity.getAdminFee());
        denom.setValidityDays(entity.getValidityDays());
        denom.setQuotaMb(entity.getQuotaMb());
        denom.setMinAmount(entity.getMinAmount());
        denom.setMaxAmount(entity.getMaxAmount());
        denom.setRequiresInquiry(entity.isRequiresInquiry());
        denom.setStockAvailable(entity.getStockAvailable());
        denom.setActive(entity.isActive());
        denom.setDeleted(entity.isDeleted());
        denom.setSortOrder(entity.getSortOrder());
        denom.setCreatedAt(entity.getCreatedAt());
        denom.setUpdatedAt(entity.getUpdatedAt());
        denom.setCreatedBy(entity.getCreatedBy());
        denom.setUpdatedBy(entity.getUpdatedBy());
        denom.setVersion(entity.getVersion());
        return denom;
    }

    public ProductDenomJpaEntity toEntity(ProductDenoms denom) {
        if (denom == null) {
            return null;
        }
        ProductDenomJpaEntity entity = new ProductDenomJpaEntity();
        entity.setId(denom.getId());
        entity.setProductId(denom.getProductId());
        entity.setCode(denom.getCode());
        entity.setName(denom.getName());
        entity.setDenomType(denom.getDenomType());
        entity.setNominal(denom.getNominal());
        entity.setPrice(denom.getPrice());
        entity.setBasePrice(denom.getBasePrice());
        entity.setAdminFee(denom.getAdminFee());
        entity.setValidityDays(denom.getValidityDays());
        entity.setQuotaMb(denom.getQuotaMb());
        entity.setMinAmount(denom.getMinAmount());
        entity.setMaxAmount(denom.getMaxAmount());
        entity.setRequiresInquiry(denom.isRequiresInquiry());
        entity.setStockAvailable(denom.getStockAvailable());
        entity.setActive(denom.isActive());
        entity.setDeleted(denom.isDeleted());
        entity.setSortOrder(denom.getSortOrder());
        entity.setCreatedAt(denom.getCreatedAt());
        entity.setUpdatedAt(denom.getUpdatedAt());
        entity.setCreatedBy(denom.getCreatedBy());
        entity.setUpdatedBy(denom.getUpdatedBy());
        entity.setVersion(denom.getVersion());
        return entity;
    }

    public List<ProductDenoms> toDomainList(List<ProductDenomJpaEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .toList();
    }

    public Optional<ProductDenoms> toOptionalDomain(Optional<ProductDenomJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}