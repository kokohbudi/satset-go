package com.satset.wallet.web.dto;

import com.satset.wallet.model.MutationReferenceType;
import com.satset.wallet.model.MutationType;
import com.satset.wallet.model.WalletMutationEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MutationResponse(
    UUID id,
    String walletId,
    BigDecimal amount,
    MutationType mutationType,
    BigDecimal balanceAfter,
    MutationReferenceType referenceType,
    UUID referenceId,
    String description,
    LocalDateTime createdAt
) {
    public static MutationResponse from(WalletMutationEntity mutation) {
        return new MutationResponse(
                mutation.getId(), mutation.getWalletId(), mutation.getAmount(), mutation.getMutationType(),
            mutation.getBalanceAfter(), mutation.getReferenceType(), mutation.getReferenceId(),
            mutation.getDescription(), mutation.getCreatedAt());
    }
}
