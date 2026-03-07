package com.omnip.wallet.adapter.in.web.dto;

import com.omnip.wallet.domain.model.MutationReferenceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletRequest(
    @NotNull UUID storeId,
    @NotNull @Positive BigDecimal amount,
    @NotNull UUID referenceId,
    MutationReferenceType referenceType,
    String description
) {}