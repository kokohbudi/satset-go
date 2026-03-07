package com.omnip.transaction.domain.port.out;

import com.omnip.transaction.domain.model.StoreMutations;

import java.util.Optional;
import java.util.UUID;

public interface StoreMutationRepositoryPort {

    StoreMutations save(StoreMutations storeMutation);

    Optional<StoreMutations> findTopByStoreIdOrderByCreatedAtDesc(UUID storeId);
}