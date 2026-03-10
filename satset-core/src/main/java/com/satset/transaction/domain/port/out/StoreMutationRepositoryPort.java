package com.satset.transaction.domain.port.out;

import com.satset.transaction.domain.model.StoreMutations;

import java.util.Optional;

public interface StoreMutationRepositoryPort {

    StoreMutations save(StoreMutations storeMutation);

    Optional<StoreMutations> findTopByWalletIdOrderByCreatedAtDesc(String walletId);
}