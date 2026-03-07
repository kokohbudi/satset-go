package com.omnip.wallet.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequest(
    @NotNull UUID storeId,
    @NotNull @Positive BigDecimal amount,
    @NotNull UUID originalReferenceId,
    String description
) {}