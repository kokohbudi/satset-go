package com.satset.transaction.adapter.out.persistence.mapper;

import com.satset.transaction.adapter.out.persistence.entity.WalletAccountJpaEntity;
import com.satset.transaction.domain.model.WalletAccount;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WalletAccountMapper {

    public WalletAccount toDomain(WalletAccountJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        WalletAccount walletAccount = new WalletAccount();
        walletAccount.setId(entity.getId());
        walletAccount.setStoreId(entity.getStoreId());
        walletAccount.setBalance(entity.getBalance());
        walletAccount.setCreatedAt(entity.getCreatedAt());
        walletAccount.setUpdatedAt(entity.getUpdatedAt());
        walletAccount.setVersion(entity.getVersion());
        return walletAccount;
    }

    public WalletAccountJpaEntity toEntity(WalletAccount walletAccount) {
        if (walletAccount == null) {
            return null;
        }
        WalletAccountJpaEntity entity = new WalletAccountJpaEntity();
        entity.setId(walletAccount.getId());
        entity.setStoreId(walletAccount.getStoreId());
        entity.setBalance(walletAccount.getBalance());
        entity.setCreatedAt(walletAccount.getCreatedAt());
        entity.setUpdatedAt(walletAccount.getUpdatedAt());
        entity.setVersion(walletAccount.getVersion());
        return entity;
    }

    public Optional<WalletAccount> toOptionalDomain(Optional<WalletAccountJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}