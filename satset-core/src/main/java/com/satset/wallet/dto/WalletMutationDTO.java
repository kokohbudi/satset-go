package com.satset.wallet.dto;

import com.satset.wallet.model.MutationReferenceType;
import com.satset.wallet.model.MutationType;
import com.satset.wallet.model.WalletMutationEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read view of a single wallet mutation (saldo ledger entry). */
public record WalletMutationDTO(MutationType type, BigDecimal amount, BigDecimal balanceAfter,
                                MutationReferenceType referenceType, String description, LocalDateTime createdAt) {

    public static WalletMutationDTO from(WalletMutationEntity m) {
        return new WalletMutationDTO(m.getMutationType(), m.getAmount(), m.getBalanceAfter(),
                m.getReferenceType(), m.getDescription(), m.getCreatedAt());
    }
}
