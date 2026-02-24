package com.omnip.catalog.domain.service;

import com.omnip.catalog.domain.model.ProductDenomMeta;
import com.omnip.catalog.domain.model.ProductDenoms;
import com.omnip.catalog.domain.model.Products;
import com.omnip.catalog.domain.port.in.BrowseDenomsUseCase;
import com.omnip.catalog.adapter.out.persistence.DenomMetaJpaRepository;
import com.omnip.catalog.adapter.out.persistence.DenomJpaRepository;
import com.omnip.catalog.adapter.out.persistence.ProductJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class DenomDomainService implements BrowseDenomsUseCase {

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
}