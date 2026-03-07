package com.omnip.wallet.adapter.out.persistence.mapper;

import com.omnip.wallet.adapter.out.persistence.entity.WalletAccountEntity;
import com.omnip.wallet.domain.model.WalletAccount;

public class WalletAccountMapper {

    private WalletAccountMapper() {}

    public static WalletAccount toDomain(WalletAccountEntity entity) {
        return new WalletAccount(entity.getId(), entity.getStoreId(), entity.getBalance(), entity.getVersion());
    }

    public static WalletAccountEntity toEntity(WalletAccount domain) {
        WalletAccountEntity entity = new WalletAccountEntity();
        entity.setId(domain.id());
        entity.setStoreId(domain.storeId());
        entity.setBalance(domain.balance());
        entity.setVersion(domain.version());
        return entity;
    }
}
