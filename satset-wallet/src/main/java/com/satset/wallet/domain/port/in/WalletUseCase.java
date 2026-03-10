package com.satset.wallet.domain.port.in;

import com.satset.wallet.domain.WalletMutationResult;
import com.satset.wallet.domain.model.MutationReferenceType;
import com.satset.wallet.domain.model.WalletAccount;
import com.satset.wallet.domain.model.WalletMutation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Use case interface for wallet operations.
 */
public interface WalletUseCase {

    /**
     * Creates a new wallet with a generated wallet ID.
     *
     * @return the created wallet account
     */
    WalletAccount createWallet();

    BigDecimal getBalance(String walletId);

    WalletMutationResult debit(String walletId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description);

    WalletMutationResult credit(String walletId, BigDecimal amount, UUID referenceId,
            MutationReferenceType referenceType, String description);

    WalletMutationResult refund(String walletId, BigDecimal amount, UUID originalReferenceId, String description);

    List<WalletMutation> getMutations(String walletId);
}
