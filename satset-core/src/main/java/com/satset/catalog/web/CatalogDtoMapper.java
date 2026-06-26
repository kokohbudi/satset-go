package com.satset.catalog.web;

import com.satset.catalog.dto.CategoryDTO;
import com.satset.catalog.dto.ProductDTO;
import com.satset.catalog.dto.ProductDenomDTO;
import com.satset.catalog.dto.ProductDenomMetaDTO;
import com.satset.catalog.model.Category;
import com.satset.catalog.model.ProductDenomMeta;
import com.satset.catalog.model.ProductDenoms;
import com.satset.catalog.model.Products;

/**
 * Shared entity -> DTO mapping for the catalog web layer.
 * Hoisted from the (previously duplicated) private mappers in the catalog controllers.
 */
public final class CatalogDtoMapper {

    private CatalogDtoMapper() {
    }

    public static CategoryDTO toCategoryDTO(Category entity) {
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

    static ProductDTO toProductDTO(Products entity) {
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

    static ProductDenomDTO toDenomDTO(ProductDenoms entity) {
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
        return dto;
    }

    static ProductDenomMetaDTO toMetaDTO(ProductDenomMeta entity) {
        ProductDenomMetaDTO dto = new ProductDenomMetaDTO();
        dto.setMetaKey(entity.getMetaKey());
        dto.setMetaValue(entity.getMetaValue());
        return dto;
    }
}
