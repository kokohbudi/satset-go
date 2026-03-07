package com.omnip.wallet.domain.port.out;

import com.omnip.wallet.domain.model.MutationReferenceType;
import com.omnip.wallet.domain.model.WalletMutation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletMutationPort {

    List<WalletMutation> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    Optional<WalletMutation> findByReferenceIdAndReferenceType(UUID referenceId, MutationReferenceType referenceType);

    WalletMutation save(WalletMutation mutation);
}