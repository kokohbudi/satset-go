package com.satset.wallet.domain.port.out;

import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.WalletMutation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletMutationPort {

    List<WalletMutation> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    Optional<WalletMutation> findByReferenceIdAndReferenceType(UUID referenceId, MutationReferenceType referenceType);

    WalletMutation save(WalletMutation mutation);
}