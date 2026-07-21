package com.satset.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PayRequest(@NotNull UUID denomId, @NotBlank String customerNo,
        BigDecimal amount, @NotNull BigDecimal expectedTotal) {
}
