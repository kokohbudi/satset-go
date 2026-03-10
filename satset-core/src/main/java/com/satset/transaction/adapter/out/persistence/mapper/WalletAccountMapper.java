package com.satset.transaction.adapter.out.persistence.mapper;

import com.satset.transaction.adapter.out.persistence.entity.WalletAccountJpaEntity;
import com.satset.transaction.domain.model.WalletAccount;
import org.mapstruct.Mapper;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface WalletAccountMapper {

    WalletAccount toDomain(WalletAccountJpaEntity entity);

    WalletAccountJpaEntity toEntity(WalletAccount walletAccount);

    default Optional<WalletAccount> toOptionalDomain(Optional<WalletAccountJpaEntity> entity) {
        return entity.map(this::toDomain);
    }
}
