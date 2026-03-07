package com.omnip.wallet.adapter.in.web.dto;

import com.omnip.wallet.domain.model.MutationReferenceType;
import com.omnip.wallet.domain.model.MutationType;
import com.omnip.wallet.domain.model.WalletMutation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MutationResponse(
    UUID id,
    UUID storeId,
    BigDecimal amount,
    MutationType mutationType,
    BigDecimal balanceAfter,
    MutationReferenceType referenceType,
    UUID referenceId,
    String description,
    LocalDateTime createdAt
) {
    public static MutationResponse from(WalletMutation mutation) {
        return new MutationResponse(
            mutation.id(), mutation.storeId(), mutation.amount(), mutation.mutationType(),
            mutation.balanceAfter(), mutation.referenceType(), mutation.referenceId(),
            mutation.description(), mutation.createdAt());
    }
}