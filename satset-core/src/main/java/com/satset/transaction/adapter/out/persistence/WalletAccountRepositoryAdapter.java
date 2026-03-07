package com.satset.transaction.adapter.out.persistence;

import com.satset.transaction.adapter.out.persistence.entity.WalletAccountJpaEntity;
import com.satset.transaction.adapter.out.persistence.mapper.WalletAccountMapper;
import com.satset.transaction.domain.model.WalletAccount;
import com.satset.transaction.domain.port.out.WalletAccountPort;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class WalletAccountRepositoryAdapter implements WalletAccountPort {

    private final WalletAccountJpaRepository jpaRepository;
    private final WalletAccountMapper mapper;

    WalletAccountRepositoryAdapter(WalletAccountJpaRepository jpaRepository, WalletAccountMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<WalletAccount> findByStoreId(UUID storeId) {
        return mapper.toOptionalDomain(jpaRepository.findByStoreId(storeId));
    }

    @Override
    public Optional<WalletAccount> findByStoreIdWithLock(UUID storeId) {
        return mapper.toOptionalDomain(jpaRepository.findByStoreIdWithLock(storeId));
    }

    @Override
    public WalletAccount save(WalletAccount walletAccount) {
        WalletAccountJpaEntity entity = mapper.toEntity(walletAccount);
        WalletAccountJpaEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}