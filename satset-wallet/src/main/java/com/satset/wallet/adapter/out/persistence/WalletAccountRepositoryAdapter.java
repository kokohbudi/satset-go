package com.satset.wallet.adapter.out.persistence;

import com.satset.wallet.adapter.out.persistence.mapper.WalletAccountMapper;
import com.satset.wallet.domain.model.WalletAccount;
import com.satset.wallet.domain.port.out.WalletAccountPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
    public Optional<WalletAccount> findByWalletId(String walletId) {
        return repository.findById(walletId).map(WalletAccountMapper::toDomain);
    }

    @Override
    public Optional<WalletAccount> findByWalletIdWithLock(String walletId) {
        return repository.findByWalletIdWithLock(walletId).map(WalletAccountMapper::toDomain);
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
