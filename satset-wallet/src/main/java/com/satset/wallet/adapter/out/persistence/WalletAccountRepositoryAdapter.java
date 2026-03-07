package com.satset.wallet.adapter.out.persistence;

import com.satset.wallet.adapter.out.persistence.mapper.WalletAccountMapper;
import com.satset.wallet.domain.model.WalletAccount;
import com.satset.wallet.domain.port.out.WalletAccountPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter for wallet account persistence using JPA.
 */
@Component
public class WalletAccountRepositoryAdapter implements WalletAccountPort {

    private final WalletAccountRepository repository;

    public WalletAccountRepositoryAdapter(WalletAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<WalletAccount> findByStoreId(UUID storeId) {
        return repository.findByStoreId(storeId).map(WalletAccountMapper::toDomain);
    }

    @Override
    public Optional<WalletAccount> findByStoreIdWithLock(UUID storeId) {
        return repository.findByStoreIdWithLock(storeId).map(WalletAccountMapper::toDomain);
    }

    @Override
    public Optional<WalletAccount> findByWalletId(String walletId) {
        return repository.findById(walletId).map(WalletAccountMapper::toDomain);
    }

    @Override
    public WalletAccount save(WalletAccount account) {
        return WalletAccountMapper.toDomain(repository.save(WalletAccountMapper.toEntity(account)));
    }

    @Override
    public boolean existsByWalletId(String walletId) {
        return repository.existsByWalletId(walletId);
    }
}
