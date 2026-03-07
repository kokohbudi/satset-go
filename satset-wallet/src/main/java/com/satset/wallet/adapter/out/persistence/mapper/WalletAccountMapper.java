package com.satset.wallet.adapter.out.persistence.mapper;

import com.satset.wallet.adapter.out.persistence.entity.WalletAccountEntity;
import com.satset.wallet.domain.model.WalletAccount;

/**
 * Mapper between WalletAccount domain model and JPA entity.
 */
public class WalletAccountMapper {

    private WalletAccountMapper() {}

    /**
     * Converts JPA entity to domain model.
     *
     * @param entity the JPA entity
     * @return domain model instance
     */
    public static WalletAccount toDomain(WalletAccountEntity entity) {
        return new WalletAccount(
                entity.getWalletId(),
                entity.getStoreId(),
                entity.getBalance(),
                entity.getVersion()
        );
    }

    /**
     * Converts domain model to JPA entity.
     *
     * @param domain the domain model
     * @return JPA entity instance
     */
    public static WalletAccountEntity toEntity(WalletAccount domain) {
        WalletAccountEntity entity = new WalletAccountEntity();
        entity.setWalletId(domain.walletId());
        entity.setStoreId(domain.storeId());
        entity.setBalance(domain.balance());
        entity.setVersion(domain.version());
        return entity;
    }
}
