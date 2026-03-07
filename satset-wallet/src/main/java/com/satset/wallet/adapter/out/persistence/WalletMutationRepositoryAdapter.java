package com.satset.wallet.adapter.out.persistence;

import com.satset.wallet.adapter.out.persistence.mapper.WalletMutationMapper;
import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.WalletMutation;
import com.satset.wallet.domain.port.out.WalletMutationPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class WalletMutationRepositoryAdapter implements WalletMutationPort {

    private final WalletMutationRepository repository;

    public WalletMutationRepositoryAdapter(WalletMutationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<WalletMutation> findByStoreIdOrderByCreatedAtDesc(UUID storeId) {
        return repository.findByStoreIdOrderByCreatedAtDesc(storeId).stream()
                .map(WalletMutationMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<WalletMutation> findByReferenceIdAndReferenceType(UUID referenceId, MutationReferenceType referenceType) {
        return repository.findByReferenceIdAndReferenceType(referenceId, referenceType)
                .map(WalletMutationMapper::toDomain);
    }

    @Override
    public WalletMutation save(WalletMutation mutation) {
        return WalletMutationMapper.toDomain(repository.save(WalletMutationMapper.toEntity(mutation)));
    }
}
