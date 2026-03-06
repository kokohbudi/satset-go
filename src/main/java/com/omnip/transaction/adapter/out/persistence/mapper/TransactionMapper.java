package com.omnip.transaction.adapter.out.persistence.mapper;

import com.omnip.transaction.adapter.out.persistence.entity.TransactionJpaEntity;
import com.omnip.transaction.domain.model.Transactions;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TransactionMapper {

    public Transactions toDomain(TransactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        Transactions transaction = new Transactions();
        transaction.setId(entity.getId());
        transaction.setStoreId(entity.getStoreId());
        transaction.setProductDenomId(entity.getProductDenomId());
        transaction.setDenomName(entity.getDenomName());
        transaction.setProductName(entity.getProductName());
        transaction.setTargetNumber(entity.getTargetNumber());
        transaction.setPrice(entity.getPrice());
        transaction.setAdminFee(entity.getAdminFee());
        transaction.setTotal(entity.getTotal());
        transaction.setStatus(entity.getStatus());
        transaction.setProviderRef(entity.getProviderRef());
        transaction.setSerialNumber(entity.getSerialNumber());
        transaction.setCreatedAt(entity.getCreatedAt());
        transaction.setUpdatedAt(entity.getUpdatedAt());
        transaction.setVersion(entity.getVersion());
        return transaction;
    }

    public TransactionJpaEntity toEntity(Transactions transaction) {
        if (transaction == null) {
            return null;
        }
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.setId(transaction.getId());
        entity.setStoreId(transaction.getStoreId());
        entity.setProductDenomId(transaction.getProductDenomId());
        entity.setDenomName(transaction.getDenomName());
        entity.setProductName(transaction.getProductName());
        entity.setTargetNumber(transaction.getTargetNumber());
        entity.setPrice(transaction.getPrice());
        entity.setAdminFee(transaction.getAdminFee());
        entity.setTotal(transaction.getTotal());
        entity.setStatus(transaction.getStatus());
        entity.setProviderRef(transaction.getProviderRef());
        entity.setSerialNumber(transaction.getSerialNumber());
        entity.setCreatedAt(transaction.getCreatedAt());
        entity.setUpdatedAt(transaction.getUpdatedAt());
        entity.setVersion(transaction.getVersion());
        return entity;
    }

    public Optional<Transactions> toOptionalDomain(Optional<TransactionJpaEntity> entity) {
        return entity.map(this::toDomain);
    }

    public Page<Transactions> toDomainPage(Page<TransactionJpaEntity> entityPage) {
        return entityPage.map(this::toDomain);
    }
}