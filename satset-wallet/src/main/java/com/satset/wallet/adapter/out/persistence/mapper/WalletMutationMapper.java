package com.satset.wallet.adapter.out.persistence.mapper;

import com.satset.wallet.adapter.out.persistence.entity.WalletMutationEntity;
import com.satset.wallet.domain.model.WalletMutation;

public class WalletMutationMapper {

    private WalletMutationMapper() {}

    public static WalletMutation toDomain(WalletMutationEntity entity) {
        return new WalletMutation(
                entity.getId(), entity.getStoreId(), entity.getAmount(),
                entity.getMutationType(), entity.getBalanceAfter(), entity.getReferenceType(),
                entity.getReferenceId(), entity.getDescription(), entity.getCreatedAt());
    }

    public static WalletMutationEntity toEntity(WalletMutation domain) {
        WalletMutationEntity entity = new WalletMutationEntity();
        entity.setStoreId(domain.storeId());
        entity.setAmount(domain.amount());
        entity.setMutationType(domain.mutationType());
        entity.setBalanceAfter(domain.balanceAfter());
        entity.setReferenceType(domain.referenceType());
        entity.setReferenceId(domain.referenceId());
        entity.setDescription(domain.description());
        return entity;
    }
}
