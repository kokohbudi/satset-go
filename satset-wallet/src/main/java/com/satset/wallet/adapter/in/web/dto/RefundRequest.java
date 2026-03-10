package com.satset.wallet.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequest(
        @NotNull String walletId,
    @NotNull @Positive BigDecimal amount,
    @NotNull UUID originalReferenceId,
    String description
) {}