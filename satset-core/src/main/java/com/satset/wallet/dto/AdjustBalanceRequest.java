package com.satset.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustBalanceRequest(
        @NotNull @DecimalMin("1") BigDecimal amount,
        String description) {
}
