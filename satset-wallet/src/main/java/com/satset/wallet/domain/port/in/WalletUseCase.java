package com.satset.wallet.domain.port.in;

import com.satset.wallet.domain.WalletMutationResult;
import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.WalletMutation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface WalletUseCase {

    BigDecimal getBalance(UUID storeId);

    WalletMutationResult debit(UUID storeId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description);

    WalletMutationResult credit(UUID storeId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description);

    WalletMutationResult refund(UUID storeId, BigDecimal amount, UUID originalReferenceId, String description);

    List<WalletMutation> getMutations(UUID storeId);
}
